package simblock.adversary;

import static simblock.simulator.Network.getEdgeLatency;
import static simblock.simulator.Simulator.debugLog;

import java.util.*;

import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparse;


import simblock.block.AbstractMessage;
import simblock.node.Node;
import simblock.node.Link;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.task.AbstractMessageTask;

public class AdversaryKnowledge {
    /** All nodes in the network($d=20$) */
    public final List<Node> knownNodes;

    public List<Node> restNodesForCentrality;

    int totalLinks = 0;

    Set<AbstractMessage> seenMessages = new HashSet<>();

    /**
     * global knowledge knows the routing table of all nodes. local knowledge
     * only knows the routing table of the corrupted nodes and the
     * getKnownNodes() from all nodes.
     */
    boolean global;

    HashSet<Node> corruptedNodes = new HashSet<>();

    /** Known links and corrupted links */
    HashMap<Node, HashSet<Node>> corruptedLinks = new HashMap<>();

    HashSet<Node> nodeCorruptionWip = new HashSet<>();

    HashMap<Node, HashSet<Node>> linkCorruptionWip = new HashMap<>();

    /** Only saves the latency between nodes, for M(i,i) or nodes without connection the value is zero. */
    // record edge weight () of the initial topology
    public DMatrixSparse latencyMatrix;

    public AdversaryKnowledge(boolean global) {
        this(global, 0);
    }

    public AdversaryKnowledge(boolean global, int numPublicNodes) {
        this.global = global;
        if (global) {
            this.knownNodes = Simulator.getSimulatedNodes();
        } else {
            List<Node> allNodes = new ArrayList<Node>(Simulator.getSimulatedNodes());
            Collections.shuffle(allNodes);
            this.knownNodes = new ArrayList<>();
            for (int i = 0; i < numPublicNodes; i++) {
                this.knownNodes.add(allNodes.get(i));
            }
            debugLog("Known nodes: " + knownNodes);
        }
        for (Node n : Simulator.getSimulatedNodes()) {
            totalLinks += n.getRoutingTable().getNeighbors().size();
        }
        totalLinks /= 2;
        prepareAdjacencyMatrix();
    }

    public boolean isGlobal() {
        return global;
    }

    private void prepareAdjacencyMatrix() {
        List<Node> allNodes = knownNodes;
        latencyMatrix = new DMatrixSparseCSC(allNodes.size(), allNodes.size());
        
        for (int i = 0; i < allNodes.size(); i++) {
            for (int j = i+1; j < allNodes.size(); j++) {
                Node m = allNodes.get(i);
                Node n = allNodes.get(j);
                if (getConnections(m).contains(n) || getConnections(n).contains(m)) {
                    // edge is undirected, but the latency on different direction differs
                    latencyMatrix.set(i, j, getEdgeLatency(m.getRegion(), n.getRegion()));
                    latencyMatrix.set(j, i, getEdgeLatency(n.getRegion(), m.getRegion()));
                }
            }
        }
    }

    /**
     * Call when the topology changes and update the latency adjacencyMatrix.
     * @return the updated adjacencyMatrix
     */
    public DMatrixSparse updateAdjacencyMatrix() {
        return latencyMatrix;
    }

    /**
     * Get the set of nodes that are the connections of node @param n.
     * 
     * @param n
     * @return null if the adversary does not have this knowledge
     */
    public List<Node> getConnections(Node n) {
        if (!knownNodes.contains(n)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(n.getRoutingTable().getNeighbors());
    }

    /**
     * Count the number of link in the link map <node->list_of_nodes>
     * @param linkMap
     * @return
     */
    public int getNumLinks(HashMap<Node, HashSet<Node>> linkMap) {
        return linkMap.values().stream().map(HashSet::size).mapToInt(i -> i).sum();
    }

    public boolean isNodeCorrupted(Node n) {
        return n.isCorrupted();
    }

    public boolean isLinkCorrupted(Link link) {
        HashSet<Node> toNodes = corruptedLinks.getOrDefault(link.getFrom(), new HashSet<Node>());
        return toNodes.contains(link.getTo());
    }

    public boolean isNodeAttacked(Node n) {
        return nodeCorruptionWip.contains(n);
    }

    public boolean isLinkAttacked(Link link) {
        if (linkCorruptionWip.containsKey(link.getFrom())) {
            return linkCorruptionWip.get(link.getFrom()).contains(link.getTo());
        }
        return false;
    }

    /**
     * @deprecated Corrupted nodes are reset for each new attack. Therefore, during attack
     * planning, we do not distinguish if a node is attacked or corrupted.
     * @return True if a node is neither corrupted nor attacked
     */
    @Deprecated
    public boolean isNodeCandidate(Node n) {
        return !isNodeAttacked(n) && !isNodeCorrupted(n);
    }
    
    @Deprecated
    public boolean isLinkCandidate(Link link) {
        return !isLinkAttacked(link) && !isLinkCorrupted(link);
    }

    public void addSeenMessage(AbstractMessageTask task) {
        if (isTargetedMessage(task)) {
            seenMessages.add(task.getMessage());
        }
    }

    /**
     * Find a list of node to be released as they are no longer on the
     * propagation path of any on-going messages
     * 
     * @return
     */
    public List<Node> getNodes2Release() {
        List<AbstractMessage> activeMessages = new ArrayList<>();
        for (AbstractMessage msg : this.seenMessages) {
            if (!msg.reachedAllHonestNodes()) {
                activeMessages.add(msg);
            }
        }
        List<Node> toRelease = new ArrayList<>();
        List<Node> nodes = new ArrayList<>(corruptedNodes);
        nodes.addAll(nodeCorruptionWip);
        for (Node n : nodes) {
            boolean useful = false;
            for (AbstractMessage msg : activeMessages) {
                if (!msg.reachedNode(n)) {
                    useful = true;
                    break;
                }
            }
            if (!useful) {
                toRelease.add(n);
            }
        }
        return toRelease;
    }

    /**
     * Predict the set of nodes who will be the next one to receive any messages
     * (and how long) when the corruption succeeds.
     * 
     * @param pendingTasks map of tasks and when they will be processed
     * @return map of nodes and when they are going to receive what message
     */
    public Map<Node, Map<AbstractMessage, Integer>> getCandidateNodes(Map<AbstractMessageTask, Integer> pendingTasks) {
        Map<Node, Map<AbstractMessage, Integer>> candidates = new HashMap<>();
        for (AbstractMessageTask task : pendingTasks.keySet()) {
            if (this.isTargetedMessage(task)) {
                Node target = task.getTo();
                Map<AbstractMessage, Integer> messages = candidates.getOrDefault(target, new HashMap<>());
                // for the same message, shortest delay is recorded
                int delay = messages.getOrDefault(task.getMessage(), Integer.MAX_VALUE);
                if (pendingTasks.get(task) < delay) {
                    messages.put(task.getMessage(), pendingTasks.get(task));
                }
                candidates.putIfAbsent(target, messages);
            }
        }
        return candidates;
    }

    public Map<AbstractMessageTask, Integer> getPendingTasks(int after) {
        if (global) {
            // adversary with global knowledge fully predict which will be the
            // nodes to process the messages after the corruption delay and set
            // those as targets.
            return Timer.getPendingMsgTasksAfterTime(after);
        } else {
            throw new UnsupportedOperationException("not implemented 'getPendingTasks' for local knowledge");
        }
    }

    public boolean isTargetedMessage(AbstractMessageTask task) {
        switch (task.getClass().getSimpleName()) {
            case "BlockMessageTask":
            case "CmpctBlockMessageTask":
            case "ShredTask":
            case "TransactionTask":
                return true;
            case "InvMessageTask":
            case "RecMessageTask":
            case "GetBlockTxnMessageTask":
            default:
                return false;
        }
    }

    public List<Node> getUnreachedNodes(AbstractMessage message) {
        List<Node> unreached = new ArrayList<>();
        for (Node n : knownNodes) {
            if (!message.reachedNode(n)) {
                unreached.add(n);
            }
        }
        return unreached;
    }

}
