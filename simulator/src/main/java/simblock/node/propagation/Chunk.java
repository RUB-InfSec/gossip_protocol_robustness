package simblock.node.propagation;

import simblock.node.Node;
import simblock.settings.NetworkConfiguration;
import simblock.simulator.Simulator;
import simblock.task.AbstractMessageTask;
import simblock.task.ShredTask;
import simblock.block.Block;
import simblock.block.Shred;
import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Timer.putTask;
import static simblock.simulator.Simulator.debugLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Chunk ignores the graph topology and creates a random tree for each shred (a
 * partition of the orig message) and propogate the shreds along the trees.
 */
public class Chunk extends AbstractPropagationProtocol {

    private final int fanout;

    /** k data chunks and m code chunks */
    private static int K;
    private static int M;

    Map<Integer, ArrayList<Shred>> receivedShreds = new HashMap<Integer, ArrayList<Shred>>();
    HashSet<Block> recoveredBlocks = new HashSet<>();

    public Chunk(Node node) {
        super(node);
        fanout = NetworkConfiguration.DEGREE;
    }

    public static void setErasureCodeConfig(int k, int m) {
        if (BLOCK_SIZE / k > SHRED_SIZE) {
            Chunk.K = (int) Math.ceil(BLOCK_SIZE / SHRED_SIZE);
            Chunk.M = Chunk.K;
            System.out.printf(">>> Reset EC config as (%d, %d)\n", Chunk.K, Chunk.M);
            return;
        }
        Chunk.K = k;
        Chunk.M = m;
    }

    /**
     * Generate a random tree for each shred seeded by
     * 
     * @param leader at @param slot and @param shred_id.
     *
     * @return the children of this node in the tree
     */
    public List<Node> getChildrenInPropagationTree(Node leader, int slot, int shred_id) {
        List<Node> nodes = new ArrayList<>(Simulator.getSimulatedNodes());
        nodes.remove(leader);
        Random rng = getSeededRng(slot, leader, shred_id);
        nodes = weightedShuffle(nodes, rng);
        if (selfNode == leader) {   // leader sends the shred to the root of the tree
            return Arrays.asList(nodes.get(0));
        }
        // return getChildrenFromFullTree(nodes, selfNode, fanout);
        return getChildrenFromStrideTree(nodes, selfNode, fanout);
    }

    /**
     * Stake-weighted analogue of {@code Collections.shuffle}: a weighted permutation without
     * replacement, mirroring Solana Turbine's real weighted-shuffle-by-stake (there implemented as
     * a Fenwick/BIT tree over cumulative stake; here via the equivalent, simpler
     * Efraimidis-Spirakis A-ExpJ algorithm). Nodes with higher {@link Node#getStakeProportion()}
     * are more likely to sort earlier -- and therefore land in shallower, lower-hop tree
     * positions -- proportional to their stake. Nodes with zero (or unset) stake are always
     * included but always placed after every staked node, uniformly shuffled among themselves,
     * exactly like Solana's unstaked-validator handling.
     * <p>
     * If every node has zero stake (e.g. {@code SIM_CONFIGS} unset, as in most unit tests, or a
     * non-PoS run) this degrades to exactly {@code Collections.shuffle(nodes, rng)} -- the prior
     * behavior -- with no special-casing needed.
     *
     * @param nodes the candidate nodes (leader already excluded)
     * @param rng   the seeded rng for this (slot, leader, shred_id); consumed in place
     * @return a new list: stake-weighted-sorted staked nodes, then uniformly-shuffled unstaked ones
     */
    static List<Node> weightedShuffle(List<Node> nodes, Random rng) {
        List<Node> staked = new ArrayList<>();
        List<Node> unstaked = new ArrayList<>();
        for (Node n : nodes) {
            if (n.getStakeProportion() > 0) {
                staked.add(n);
            } else {
                unstaked.add(n);
            }
        }
        if (!staked.isEmpty()) {
            // log-space A-ExpJ key: log(u) / w, a monotonic-equivalent rewrite of u^(1/w) that
            // avoids floating-point underflow to a shared 0.0 for the long-tailed stake
            // distribution's many low-weight nodes (which u^(1/w) suffers from directly).
            Map<Node, Double> keys = new HashMap<>();
            for (Node n : staked) {
                double u = rng.nextDouble();
                double w = n.getStakeProportion();
                keys.put(n, Math.log(u) / w);
            }
            staked.sort((a, b) -> Double.compare(keys.get(b), keys.get(a)));
        }
        if (!unstaked.isEmpty()) {
            Collections.shuffle(unstaked, rng);
        }
        staked.addAll(unstaked);
        return staked;
    }

    /**
     * Suppose the each node has either 0 or k child nodes except one node.
     * Nodes are stored sequentially from level 0 and return the child nodes of
     * `parent`.
     *
     * @param <T>
     * @param nodes
     * @param parent
     * @param k
     * @return
     */
    public static <T> List<T> getChildrenFromFullTree(List<T> nodes, T parent, int k) {
        int pos = nodes.indexOf(parent);
        int l = 0; // level
        int numNodes = 1;
        while (pos + 1 > numNodes) {
            l++;
            numNodes += (int) Math.pow(k, l);
        } // 1 + k^1 + k^2 +...+ k^(l-1) < pos <= 1 + k^1 + k^2 +...+ k^l
          // selfNode at level l
        int siblingStart = numNodes - (int) Math.pow(k, l);
        int siblingEnd = numNodes;
        int childrenStart = siblingEnd + (pos - siblingStart) * k;
        int childrenEnd = siblingEnd + (pos + 1 - siblingStart) * k;
        if (childrenStart >= nodes.size()) {
            return null;
        }
        if (childrenEnd > nodes.size()) {
            childrenEnd = nodes.size();
        }
        return nodes.subList(childrenStart, childrenEnd);
    }

    /**
     * Turbine's real fan-out formula (Solana's {@code get_retransmit_peers}, reduced to a closed
     * form for an indexable list): rather than a contiguous block per parent, each parent claims a
     * stride-{@code fanout} "column" through the next level, so one parent's failure is spread
     * thinly across the next layer instead of taking out one contiguous run of it. Unlike
     * {@link #getChildrenFromFullTree}, this needs no level-tracking loop at any depth.
     * <p>
     * <b>Not currently called from {@link #getChildrenInPropagationTree} or anywhere else in
     * production code.</b> {@code getChildrenFromFullTree} (contiguous) remains the active
     * fan-out formula; this is a ready, independently tested alternative -- switching to it is a
     * one-line change of which method {@code getChildrenInPropagationTree} calls.
     *
     * @param <T>
     * @param nodes  the flat node list (index 0 is this tree's own root)
     * @param parent the node whose children to compute
     * @param fanout the branching factor
     * @return the children of {@code parent}, or {@code null} if it has none left in range
     */
    public static <T> List<T> getChildrenFromStrideTree(List<T> nodes, T parent, int fanout) {
        int index = nodes.indexOf(parent);
        int offset = index == 0 ? 0 : (index - 1) % fanout;
        int anchor = index - offset;
        int step = index == 0 ? 1 : fanout;
        int start = anchor * fanout + offset + 1;
        List<T> children = new ArrayList<>();
        for (int i = 0; i < fanout; i++) {
            int childIdx = start + i * step;
            if (childIdx >= nodes.size()) {
                break;
            }
            children.add(nodes.get(childIdx));
        }
        return children.isEmpty() ? null : children;
    }

    /**
     * generate a rng with fixed seed @param slot || @param leader id || @param
     * shred_id to use in weigted shuffle
     * based on
     * https://github.com/solana-labs/solana/blob/master/ledger/src/shred.rs#L282
     * 
     * @return seeded java.util.Random instance
     */
    private static Random getSeededRng(int slot, Node leader, int shred_id) {
        Long seed = Long.valueOf("" + slot + "" + leader.getNodeID() + "" + shred_id);
        return new Random(seed);
    }

    @Override
    public void propagate(ArrayList<Node> neigbors, Block block) {
    }

    @Override
    public void processBlock(Block block) {
        if (selfNode.processBlock(block)) {
            if (block.getMinter() == selfNode) {
                propagate(block);
            }
        }
    }

    /**
     * Leader, generate shreds and sends each to the corresponding root node
     */
    public void propagate(Block block) {
        ArrayList<Shred> shreds = generateShreds(block);
        for (Shred s : shreds) {
            List<Node> root = getChildrenInPropagationTree(selfNode, s.slot, s.getId());
            sendShred(root.get(0), s);
        }
    }

    public void propagateShred(Shred shred) {
        List<Node> children = getChildrenInPropagationTree(shred.leader, shred.slot, shred.getId());
        if (children != null) {
            for (Node ch : children) {
                sendShred(ch, shred);
            }
        }
    }

    protected void sendShred(Node n, Shred s) {
        if (!n.isOnline()) {
            return;
        }
        debugLog(String.format("Node %s -> node %s: %s", this.selfNode, n, s.getMessageName()));
        ShredTask t = new ShredTask(selfNode, n, s);
        putTask(t);
    }

    @Override
    public void handleReceivedMessage(AbstractMessageTask message) {
        if (selfNode.isCorrupted() || !selfNode.isOnline()) {
            return;
        }
        if (message instanceof ShredTask) {
            // TODO count hop and update state
            Shred shred = ((ShredTask) message).getShred();
            debugLog(String.format("Node %s handle %s from node %s", this.selfNode,
                    shred.getMessageName(), message.getFrom()));
            ArrayList<Shred> shreds = this.receivedShreds.getOrDefault(shred.slot, new ArrayList<Shred>());
            shred.block.increaseHop(message.getFrom(), selfNode);
            if (shreds.isEmpty()) {
                shreds.addAll(Collections.nCopies(K + M, null));
                selfNode.updateRumorState(shred.block, 1);
            }
            if (handleShred(shred, shreds)) {
                // block does not have a previous relay node, as every shred has a different tree.
                shred.block.increaseRelay(message.getFrom(), selfNode);
                selfNode.processBlock(shred.block); // process block after the increaseRelay
            }
        } else {
            System.err.println("Unknown message task to process: " + message.getClass().getName());
        }
    }

    /**
     * Generate k data shreds and c coding shreds from a block.
     * 
     * @param block
     * @return list of shreds
     */
    private ArrayList<Shred> generateShreds(Block block) {
        ArrayList<Shred> shreds = new ArrayList<>(K + M);
        int shredSize = (int) Math.ceilDiv(BLOCK_SIZE, K);
        for (int i = 0; i < K; i++) {
            Shred s_data = new Shred(i, "DATA", block.getId(), shredSize, this.selfNode, block);
            shreds.add(i, s_data);
        }
        for (int i = K; i < K + M; i++) {
            Shred s_code = new Shred(i, "CODE", block.getId(), shredSize, this.selfNode, block);
            shreds.add(i, s_code);
        }
        return shreds;
    }

    private boolean verifyShred(Shred s, List<Shred> shreds) {
        // TODO implement verification
        return true;
    }

    /**
     * Handle the shred and try to recover the block. Returns true if a block is
     * recovered.
     * 
     * @param s
     * @param shreds
     * @return
     */
    private boolean handleShred(Shred s, ArrayList<Shred> shreds) {
        if (this.recoveredBlocks.contains(s.block)) {
            return false;
        }
        if (shreds.contains(s)) {
            System.err.printf("[WARNING] Node %d has already received %s from another node.\n",
                    selfNode.getNodeID(), s.getMessageName());
            return false;
        }
        if (!verifyShred(s, shreds)) {
            return false;
        }

        shreds.add(s.getId(), s);
        this.receivedShreds.putIfAbsent(s.slot, shreds);

        int count = 0;
        for (Shred r : shreds) {
            if (r != null) {
                count++;
            }
        }
        this.propagateShred(s);
        // Block recovered
        if (count >= K) {
            this.recoveredBlocks.add(s.block);
            return true;
        }
        return false;
    }
}
