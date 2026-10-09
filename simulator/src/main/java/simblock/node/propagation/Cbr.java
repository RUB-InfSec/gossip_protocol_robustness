package simblock.node.propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import simblock.block.Block;
import simblock.block.Transaction;
import simblock.block.AbstractMessage;
import simblock.node.Node;
import simblock.task.*;
import simblock.simulator.Timer;

import static simblock.settings.NetworkConfiguration.LATENCY;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CHURN_NODE;
import static simblock.settings.SimulationConfiguration.CBR_FAILURE_RATE_FOR_CONTROL_NODE;
import static simblock.settings.SimulationConfiguration.CBR_HIGH_BW_CONNECTIONS;
import static simblock.settings.SimulationConfiguration.COMPACT_BLOCK_SIZE;
import static simblock.simulator.Network.getBandwidth;
import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

/**
 * Class to implement Bitcoin Compact-Block propagation
 */
public class Cbr extends Advertisement {
    public ArrayList<Node> highBandwidthModeConnections = new ArrayList<>();
    public ArrayList<Node> lowBandwidthModeConnections = new ArrayList<>();

    /**
     * hashmap of all received invs for a block
     */
    protected HashMap<AbstractMessage, ArrayList<Node>> receivedCmptBlk = new HashMap<>();

    public Cbr(Node node) {
        super(node);
        useCBR = true;
    }

    /**
     * Add the message `m` to knownMessage set as well as from whom has the node
     * heard from the message.
     * In this way the node can avoid sending messages to these nodes.
     */
    public void addReceivedCmptBlk(AbstractMessage msg, Node n) {
        ArrayList<Node> nodes = this.receivedCmptBlk.getOrDefault(msg, new ArrayList<Node>());
        if (!nodes.contains(n)) {
            nodes.add(n);
        }
        this.receivedCmptBlk.putIfAbsent(msg, nodes);
    }

    @Override
    protected boolean setMsgProcessed(AbstractMessageTask message) {
        if (!super.setMsgProcessed(message)) {
            return false;
        }
        AbstractMessage msg = this.getDataMsg(message); // msg not null here
        // The block/tx will be handled, the INV sender are also marked as knowing
        // the msg and thus won't be broadcast after the msg is processed.
        for (Node n : this.receivedCmptBlk.getOrDefault(msg, new ArrayList<Node>())) {
            this.addKnownMessage(msg, n);
        }
        this.receivedCmptBlk.remove(msg);

        return true;
    }

    /**
     * choose 3 neighbors to send blocks with highbandwidht mode to selfnode
     * based on
     * https://github.com/bitcoin/bips/blob/master/bip-0152.mediawiki#user-content-Implementation
     * (with simple checking for the conenctions with the best expected propagation
     * times )
     */
    public static void categorizeConnectionsByBandwidth(List<Node> nodes) {
        for (Node n : nodes) {
            ArrayList<Node> sortedNeigborsByDelay = (ArrayList<Node>) n.getNeighbors();
            Collections.sort(sortedNeigborsByDelay, new Comparator<Node>() {
                @Override
                public int compare(Node node1, Node node2) {
                    Long bw1 = (Long) getBandwidth(node1.getRegion(), n.getRegion());
                    Long bw2 = (Long) getBandwidth(node2.getRegion(), n.getRegion());
                    Long delay1 = LATENCY[node1.getRegion()][n.getRegion()] + (COMPACT_BLOCK_SIZE / bw1);
                    Long delay2 = LATENCY[node2.getRegion()][n.getRegion()] + (COMPACT_BLOCK_SIZE / bw2);
                    return delay2.compareTo(delay1);
                }
            });
            for (int i = 0; i < CBR_HIGH_BW_CONNECTIONS && i < n.getNeighbors().size(); i++) {
                ((Cbr) sortedNeigborsByDelay.get(i).getPropagationProtocol()).highBandwidthModeConnections.add(n);
            }
        }
        for (Node n : nodes) {
            ((Cbr) n.getPropagationProtocol()).lowBandwidthModeConnections = (ArrayList<Node>) n.getNeighbors();
            ((Cbr) n.getPropagationProtocol()).lowBandwidthModeConnections
                    .removeIf(x -> ((Cbr) n.getPropagationProtocol()).highBandwidthModeConnections.contains(x));
        }
    }

    /**
     * Propagate to neighbors that have not seen the block.
     * On first call calculate the low bandwidth connections.
     * than send CmpctBlock/Inv messages accordingly
     */
    @Override
    public void propagate(ArrayList<Node> neighbors, Block block) {
        Collections.shuffle(neighbors, simRng);
        ArrayList<Node> lowBWNeighbors = new ArrayList<Node>(neighbors);
        lowBWNeighbors.removeIf(n -> !lowBandwidthModeConnections.contains(n));
        ArrayList<Node> highBWNeighbors = new ArrayList<Node>(neighbors);
        highBWNeighbors.removeIf(n -> !highBandwidthModeConnections.contains(n));

        debugLog(String.format("Node %s send to: ", selfNode) + neighbors + ", high:"
                + highBWNeighbors + ", low: " + lowBWNeighbors);

        if (selfNode.isCorrupted()) {
            this.sendInv(block, neighbors);
        } else {
            this.sendInv(block, lowBWNeighbors);
            this.sendBlock(block, highBWNeighbors);
        }
    }

    @Override
    public void handleReceivedMessage(AbstractMessageTask message) {
        super.handleReceivedMessage(message);
        if (message instanceof CmpctBlockMessageTask) {
            Block block = ((CmpctBlockMessageTask) message).getBlock();
            if (selfNode.isCorrupted()) {
                this.sendInv(block, new ArrayList<Node>(this.selfNode.getNeighbors()));
            }
            // already processed the message
            if (this.knownMessages.containsKey(block)) {
                return;
            }
            handleCompactBlockMessage((CmpctBlockMessageTask) message);
        }
        if (selfNode.isCorrupted()) {
            return;
        }
        if (message instanceof GetBlockTxnMessageTask) {
            handleGetBlockTxnMessage((GetBlockTxnMessageTask) message);
        }
    }

    /**
     *
     * called if the recieved message is a CompactBlockMessage
     * first checks if the recieved block is valid - invalid blocks can apear from
     * forks or heavily delayed blocks
     *
     * next check for a successfull compact block transmission by either check if
     * all transactions in the block are known by the node
     * or by using a probabilistic aproach
     *
     * on success handle block arrival
     * on failure send a getBlockTxnMessage
     */
    public void handleCompactBlockMessage(CmpctBlockMessageTask message) {
        Block block = message.getBlock();
        debugLog(String.format("Node %s handle CompactBlock (%s) from %s", this.selfNode, block,
                message.getFrom()));
        this.addReceivedCmptBlk(block, message.getFrom());
        if (!this.selfNode.getConsensusAlgo().isReceivedBlockValid(block, this.selfNode.getBlock())) {
            return;
        }
        boolean success;
        if (this.useTransactions()) {
            success = true;
            for (Transaction t : block.getTransactions()) {
                if (!this.knownMessages.containsKey(t)) {
                    success = false;
                    break;
                }
            }
        } else {
            float CBRfailureRate = this.selfNode.isChurnNode ? CBR_FAILURE_RATE_FOR_CHURN_NODE
                    : CBR_FAILURE_RATE_FOR_CONTROL_NODE;
            success = simRng.nextDouble() > CBRfailureRate;
        }
        if (success) {
            this.addKnownMessage(block, message.getFrom());
            block.increaseRelay(message.getFrom(), selfNode);
            this.downloadingBlocks.remove(block);
            this.processBlock(block);
        } else {
            // equivalent to GETDATA (RecMessage), now ask for missing tx
            debugLog(String.format("Node %s -> node %s: GetBlockTxn (%s)", this.selfNode,
                    message.getFrom(), block));
            block.increaseHop(message.getFrom(), selfNode);
            selfNode.updateRumorState(block, 1);
            AbstractMessageTask task = new GetBlockTxnMessageTask(this.selfNode, message.getFrom(), block);
            Timer.putTask(task);
            this.downloadingBlocks.add(block);
        }
    }

    // TODO NOTE: can also have adversarial behavior that delays messages
    /**
     * Called if the received message is a GetBLockTxnMessage (from a failed
     * compactblock transmission).
     * Starts propagation of the full block with reduced size
     *
     * @param message
     */
    public void handleGetBlockTxnMessage(GetBlockTxnMessageTask message) {
        debugLog(String.format("Node %s handle GetBLockTxn (%s) from %s", this.selfNode,
                message.getBlock(), message.getFrom()));
        (message.getBlock()).increaseHop(selfNode, message.getFrom());
        msgTransMechanism.addTransmissionTask(message);
    }

    @Override
    public void clear() {
        super.clear();
        this.receivedCmptBlk.clear();
        this.lowBandwidthModeConnections.clear();
        this.highBandwidthModeConnections.clear();
    }
}
