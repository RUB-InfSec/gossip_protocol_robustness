package simblock.node.routing;

import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.util.SupplierUtil;
import simblock.node.Node;
import simblock.node.consensus.RumorSpreading;
import simblock.settings.Config.GossipSubOpt;
import simblock.settings.NetworkConfiguration;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Simulator;
import simblock.task.GossipHeartbeatTask;
import simblock.simulator.Timer;
import simblock.task.PeerScoreDecayTask;

import java.util.*;
import java.util.function.Supplier;

import static simblock.simulator.Simulator.*;

public class GossipsubRoutingTable extends RandomGraphRoutingTable {

//     /** full-peer connections that create a mesh */
//     HashSet<Node> meshNeighbors = new HashSet<>();
//     /** metadata-only connections that only gossip (pull) periodically */
//     HashSet<Node> gossipNeighbors = new HashSet<>();

    HashMap<Node, PeerScore> peerScoreRecords = new HashMap<>();

    static GossipSubOpt conf;

    public static void setConfig(int degree, GossipSubOpt opt) {
        GossipHeartbeatTask.setHeartbeatInterval(opt.gossipHeartbeatIntervalMS, opt.numGossipPeers);
        PeerScoreDecayTask.setDecayInterval(PeerScore.DecayInterval * 1000);
        opt.meshTarget = degree;
        opt.meshLo = degree - 2;
        opt.meshHi = degree + 4;
        conf = opt;
    }

    public static void generateMeshGraph(int networkSize) {
        if (graph == null) {
            // subgraph of mesh connections is like a random graph
            // vertex are integers starting from 1 just like the node IDs
            Supplier<Integer> vertextSupplier = SupplierUtil.createIntegerSupplier(1);
            graph = new SimpleGraph<>(vertextSupplier, SupplierUtil.createDefaultEdgeSupplier(), false);
            double p = (double) conf.meshTarget / networkSize;
            GnpRandomGraphGenerator<Integer, DefaultEdge> generator = new GnpRandomGraphGenerator<>(networkSize, p, simRng.nextLong());
            generator.generateGraph(graph);
        }

        // bring any node whose degree < meshLo or > meshHi to targeted
        for (int i = 1; i <= networkSize; i++) {
            ArrayList<DefaultEdge> edges = new ArrayList<>(graph.incomingEdgesOf(i));
            while (edges.size() < conf.meshLo) {
                int newNodeId = simRng.nextInt(networkSize) + 1;
                while (newNodeId == i || graph.incomingEdgesOf(newNodeId).size() >= conf.meshHi) {
                    // find a new node whose degree is below the highwatermark
                    newNodeId = simRng.nextInt(networkSize) + 1;
                }
                graph.addEdge(i, newNodeId);
                edges = new ArrayList<>(graph.incomingEdgesOf(i));
            }
            while (edges.size() > conf.meshHi) {
                while (true) {
                    Collections.shuffle(edges, simRng);
                    DefaultEdge rndEdge = edges.get(0);
                    int otherNodeId = graph.getEdgeSource(rndEdge);
                    if (otherNodeId == i) {
                        otherNodeId = graph.getEdgeTarget(rndEdge);
                    }
                    if (graph.incomingEdgesOf(otherNodeId).size() > conf.meshLo) {
                        graph.removeEdge(rndEdge);
                        break;
                    }
                }
                edges = new ArrayList<>(graph.incomingEdgesOf(i));
            }
        }
        debugLog("---- GossipSub graph generation completed.");
    }

    public GossipsubRoutingTable(Node n) {
        super(n);
    }

    @Override
    public void initTable() {
        for (DefaultEdge edge : graph.outgoingEdgesOf(selfNode.getNodeID())) {
            addConnectionTo(allNodes.get(graph.getEdgeTarget(edge)-1));
        }
    }

    /**
     * Establish a new connection to a node and create a new peer score record
     * for that node.
     */
    @Override
    public boolean addConnectionTo(Node to) {
        if (super.addConnectionTo(to)) {
            PeerScore rec = peerScoreRecords.get(to);
            if (rec == null) {
                peerScoreRecords.put(to, new PeerScore());
            } else {    // score was retained because was negative
                rec.setMeshStatusActive();
            }
            return true;
        }
        return false;
    }

    /**
     * Remove a connection to a node and discard the corresponding peer score record if positive.
     */
    @Override
    public void removeConnection(Node to) {
        double score = getPeerScore(to);
        // retain the score if negative, s.t. malicious nodes cannot reset its score by reconnection
        // see https://docs.rs/libp2p-gossipsub/0.49.0/src/libp2p_gossipsub/peer_score.rs.html#555
        if (score > 0) {
            peerScoreRecords.remove(to);
        } else if (score < 0) { // if the score is retained, reset the `first_message_delivery` counter and apply the mesh delivery penalties
            PeerScore rec = peerScoreRecords.get(to);
            rec.firstMessageDeliveries = 0.0;
            if (neighbors.contains(to) && rec.meshMessageDeliveriesActive
                    && rec.meshMessageDeliveries < PeerScore.MeshMessageDeliveriesThreshold) {
                double deficit = PeerScore.MeshMessageDeliveriesThreshold - rec.meshMessageDeliveries;
                rec.meshFailurePenalty += deficit * deficit;
            }
            rec.setMeshStatusInactive();
        }

        super.removeConnection(to);
    }

    /**
     * Prune and graft nodes if needed.
     */
    @Override
    public Set<Node> updateConnections(Set<Node> connections) {
        Set<Node> excludedNodes = getNeighbors();
        excludedNodes.removeAll(connections);

        updateConnectionStatus();
        if (!selfNode.isCorrupted()) {
            pruneAndGraft();
        }
        Set<Node> updatedConn = getNeighbors();
        updatedConn.removeAll(excludedNodes);
        return updatedConn;
    }

    public void pruneAndGraft() {
        // first prune all nodes whose score is negative
        for (Node n: getNeighbors()) {
            if (isPeerScoreBelowThreshold(n, 0.0)) {
//                if (!SimulationConfiguration.ADVERSARY_ON && !SimulationConfiguration.CHURN_ON) {
//                    System.out.printf("Prune nodes when there is no adversary and no churn!! Network size: %d, arrival rate: %.2f, target degree: %d, failed time: %d ms\n",
//                            Simulator.getSimulatedNodes().size(), RumorSpreading.getArrivalRate(), conf.meshTarget, Timer.getCurrentTime());
//                    throw new RuntimeException("Prune nodes when there is no adversary.");
//                }
                if (n.isCorrupted()) {
                    statsGossipsubPruneCorrupted++;
                    debugLog(String.format("%s prunes corrupted node %s score below 0.0\n", selfNode, n));
//                    System.out.println(peerScoreRecords.get(n).getRecordString());
                } else {
                    statsGossipsubPruneHonest++;
                    debugLog(String.format("%s prunes honest node %s score below 0.0\n", selfNode, n));
                }
                removeConnection(n);
            }
        }
        if (getDegree() > conf.meshHi) {
            prune();
        } else if (getDegree() < conf.meshLo) {
            graft();
        }
    }

    /**
     * prune random nodes until D <= D_high
     */
    private void prune() {
        List<Node> neighborLst = new ArrayList<>(this.neighbors);
        while (neighborLst.size() > conf.meshHi) {
            Node rndNode = neighborLst.get(simRng.nextInt(neighborLst.size()));
            removeConnection(rndNode);
            neighborLst = new ArrayList<>(this.neighbors);
        }
    }

    /**
     * graft random nodes with positive score until D >= D_low
     */
    private void graft() {
        List<Node> neighborLst = new ArrayList<>(this.neighbors);
        int trials = 0;
        final int trialLimit = 2*Simulator.getSimulatedNodes().size();
        while (neighborLst.size() < conf.meshLo && trials < trialLimit ) {
            Node rndNode = pickRandomNodeExcept(selfNode);
            if (!isPeerScoreBelowThreshold(rndNode, 0.0)) {
                if (addConnectionTo(rndNode)) {
                    neighborLst = new ArrayList<>(this.neighbors);
                }
            }
            trials++;
        }
        if (trials == trialLimit && !selfNode.isCorrupted()) {
            if (statsGossipsubFailGraftTime < 0) {
                System.out.printf("Honest node cannot graft any node with peer score > 0. Network size: %d, arrival rate: %.2f, target degree: %d, churn rate: %f, corruption bound: %f, failed time: %d ms\n",
                        Simulator.getSimulatedNodes().size(), RumorSpreading.getArrivalRate(),  conf.meshTarget, NetworkConfiguration.CHURN_RATE, SimulationConfiguration.SIM_CONFIGS.adversary.pFailedNodes, Timer.getCurrentTime());
                statsGossipsubFailGraftTime = Timer.getCurrentTime() / 1000.0;
            }
//            throw new RuntimeException("Cannot graft nodes to reach low watermark.");
        }
    }

    @Override
    public void reconnect2Network() {
        this.neighbors.clear();
        float prob = (float) conf.meshTarget / allNodes.size();
        for (Node node: allNodes) {
            if (simRng.nextDouble() < prob) {
                addConnectionTo(node);
            }
        }
        pruneAndGraft();
    }

    public enum PeerScoreParam {
        TimeInMesh,
        FirstMessageDeliveries,
        MeshMessageDeliveries,
        MeshFailurePenalty,
        InvalidMessageDeliveries
    }

    /**
     * Each node keeps a peer score array of the other nodes that are in the
     * mesh connections.
     */
    public static class PeerScore {
        // time since grafted, in second
        private double graftTime = -1.0;
        // time a peer has been in the mesh since grafted, in second
        private double timeInMesh;
        // number of messages first delivered by the peer
        double firstMessageDeliveries;
        // expected message delivery rate within the window
        double meshMessageDeliveries;
        // counts number of mesh message delivery failures. When a peer is
        // pruned with a negative score, the parameter is augmented by the rate
        // deficit at the time of prune.
        double meshFailurePenalty;
        // transmit invaild messages
        double invalidMessageDeliveries; // NOT USED: no invalid message in our model

        // true if the node is in the mesh for at least MeshMessageDeliveriesActivation duration.
        boolean meshMessageDeliveriesActive = false;

        static final HashMap<PeerScoreParam, Double> weights = new HashMap<>() {{
          put(PeerScoreParam.TimeInMesh, 0.0027);
          put(PeerScoreParam.FirstMessageDeliveries, 0.664);
          put(PeerScoreParam.MeshMessageDeliveries, -0.25);
          put(PeerScoreParam.MeshFailurePenalty, -0.25);
          put(PeerScoreParam.InvalidMessageDeliveries, -99.0);
        }};
        static final HashMap<PeerScoreParam, Double> caps = new HashMap<>() {{
            put(PeerScoreParam.TimeInMesh, 3600.0);
            put(PeerScoreParam.FirstMessageDeliveries, 1500.0);
            put(PeerScoreParam.MeshMessageDeliveries, 400.0);
        }};
        static final HashMap<PeerScoreParam, Double> decays = new HashMap<>() {{
            put(PeerScoreParam.FirstMessageDeliveries, 0.9916);
            put(PeerScoreParam.MeshMessageDeliveries, 0.997);
            put(PeerScoreParam.MeshFailurePenalty, 0.997);
            put(PeerScoreParam.InvalidMessageDeliveries, 0.9994);
        }};

        static final double DecayToZero = 0.1; // set to 0 if less than this threshold
        static final double TimeInMeshQuantum = 1; // 1 second
        static final double MeshMessageDeliveriesThreshold = 10.0;
        static final double MeshMessageDeliveriesActivation = 60;   // activate mesh message deliveries if the peer has been in the mesh for 1 minute
        static final double MeshMessageDeliveryWindow = 5; // in ms
        static final int DecayInterval = 1; // 1 second

        public PeerScore() {
            setMeshStatusActive();
        }

        final void setMeshStatusActive() {
            if (!isMeshStatusActive()) {
                graftTime = Timer.getCurrentTime() / 1000.0;
                timeInMesh = 0.0;
                meshMessageDeliveriesActive = false;
            }
        }

        final void setMeshStatusInactive() {
            graftTime = -1.0;
            timeInMesh = -1.0;
            meshMessageDeliveriesActive = false;
        }

        final boolean isMeshStatusActive() {
            return graftTime >= 0;
        }

        // Compute the peer score
        double getScore() {
            double score = 0.0;
            // p1
            if (isMeshStatusActive()) {
                score += Math.min(timeInMesh, PeerScore.caps.get(PeerScoreParam.TimeInMesh))  * PeerScore.weights.get(PeerScoreParam.TimeInMesh);
            }
            // p2
            score += Math.min(firstMessageDeliveries, PeerScore.caps.get(PeerScoreParam.FirstMessageDeliveries)) * PeerScore.weights.get(PeerScoreParam.FirstMessageDeliveries) ;
            // p3a
            if (meshMessageDeliveriesActive) {
                if (meshMessageDeliveries < PeerScore.MeshMessageDeliveriesThreshold) {
                    double deficit = PeerScore.MeshMessageDeliveriesThreshold - meshMessageDeliveries;
                    score += Math.pow(deficit, 2) * PeerScore.weights.get(PeerScoreParam.MeshMessageDeliveries);
                }
            }
            // p3b
            score += meshFailurePenalty * PeerScore.weights.get(PeerScoreParam.MeshFailurePenalty);
            // p4
            score += invalidMessageDeliveries * PeerScore.weights.get(PeerScoreParam.InvalidMessageDeliveries);
            // does not consider p5 and p6
            return score;
        }

        String getRecordString() {
            return String.format("Score: %f -- MeshStatus: %s, TimeInMesh: %f s, #FirstMsg: %f, #MeshMsg: %f (%s), MeshFailurePenalty: %f, #InvalidMsg: %f",
                    getScore(),
                    isMeshStatusActive()? "active":"inactive",
                    timeInMesh,
                    firstMessageDeliveries,
                    meshMessageDeliveries,
                    meshMessageDeliveriesActive? "active":"inactive",
                    meshFailurePenalty,
                    invalidMessageDeliveries);
        }

        // Refresh the peer score at `DecayInterval` considering the decay
        // coefficient, and update the mesh time and activate the mesh message
        // delivery parameter if needed.
        public boolean refreshScore() {
            if (!isMeshStatusActive()) {
                // retained scores does not decay, preventing malicious nodes reset its negative scores by reconnecting.
                return false;
            }
            firstMessageDeliveries *= PeerScore.decays.get(PeerScoreParam.FirstMessageDeliveries);
            if (firstMessageDeliveries < DecayToZero) {
                firstMessageDeliveries = 0.0;
            }
            meshMessageDeliveries *= PeerScore.decays.get(PeerScoreParam.MeshMessageDeliveries);
            if (meshMessageDeliveries < DecayToZero) {
                meshMessageDeliveries = 0.0;
            }
            meshFailurePenalty *= PeerScore.decays.get(PeerScoreParam.MeshFailurePenalty);
            if (meshFailurePenalty < DecayToZero) {
                meshFailurePenalty = 0.0;
            }
            invalidMessageDeliveries *= PeerScore.decays.get(PeerScoreParam.InvalidMessageDeliveries);
            if (invalidMessageDeliveries < DecayToZero) {
                invalidMessageDeliveries = 0.0;
            }
            // update mesh time
            if (isMeshStatusActive()) {
                timeInMesh = Timer.getCurrentTime()/1000.0 - graftTime;
                if (timeInMesh > PeerScore.MeshMessageDeliveriesActivation) {
                    meshMessageDeliveriesActive = true;
                }
            }
            return true;
        }

        void markFirstMsgDelivery() {
            firstMessageDeliveries = Math.min(PeerScore.caps.get(PeerScoreParam.FirstMessageDeliveries), firstMessageDeliveries + 1);
            meshMessageDeliveries = Math.min(PeerScore.caps.get(PeerScoreParam.MeshMessageDeliveries), meshMessageDeliveries + 1);
        }

        void markDupMsgDelivery(int validateTimeInMs) {
            if (Timer.getCurrentTime() <= validateTimeInMs + PeerScore.MeshMessageDeliveryWindow) {
                meshMessageDeliveries = Math.min(PeerScore.caps.get(PeerScoreParam.MeshMessageDeliveries), meshMessageDeliveries + 1);
            } else {
//                debugLog("markDupMsgDelivery PASS, fall out of validation window.");
            }
        }

    }

    public void markFirstMsgDelivery(Node from) {
        PeerScore rec = peerScoreRecords.get(from);
        if (rec != null) {
            rec.markFirstMsgDelivery();
            debugLog(String.format("markFirstMsg ==> %s: score of %s = %f", selfNode, from, rec.getScore()));
        }
    }

    public void markDupMsgDelivery(Node from, int validateTimeInMs) {
        PeerScore rec = peerScoreRecords.get(from);
        if (rec != null) {
            rec.markDupMsgDelivery(validateTimeInMs);
            debugLog(String.format("markDupMsg ==> %s: score of %s = %f", selfNode, from, rec.getScore()));
        }
    }

    public void refreshScores() {
        for (Node n: peerScoreRecords.keySet()) {
            PeerScore score = peerScoreRecords.get(n);
            if (score.refreshScore() ) {
//                debugLog(String.format("--- %s: Refresh Score of %s: %s", selfNode, n, score.getRecordString()));
                Simulator.updateNodeScore(n, selfNode, score.getScore());
            }
        }
    }

    boolean isPeerScoreBelowThreshold(Node node, double threshold) {
        PeerScore rec = peerScoreRecords.get(node);
        if (rec == null) {
            return false;
        }
        return rec.getScore() < threshold;
    }

    public double getPeerScore(Node n) {
        PeerScore rec = peerScoreRecords.get(n);
        if (rec == null) {
            if (this.neighbors.contains(n)) {
                // in mesh but missing score
                throw new RuntimeException(String.format("%s in the neighbor list of %s but does not have peer score record.", n, selfNode));
            } else {
                // not in mesh with no peer score
                return 0.0;
            }
        }
        return rec.getScore();
    }

    public boolean isMeshMessageDeliveriesActive(Node n) {
        PeerScore rec = peerScoreRecords.get(n);
        if (rec == null || !rec.isMeshStatusActive()) {
            return false;
        }
        return rec.meshMessageDeliveriesActive;
    }
}
