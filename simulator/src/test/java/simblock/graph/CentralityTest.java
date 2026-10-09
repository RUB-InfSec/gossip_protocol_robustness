package simblock.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import simblock.adversary.AdversaryKnowledge;
import simblock.graph.centrality.BetweennessCentrality;
import simblock.graph.centrality.ClosenessCentrality;
import simblock.graph.centrality.DegreeCentrality;
import simblock.node.Node;
import simblock.node.routing.UndirectedRoutingTable;
import simblock.simulator.Network;
import simblock.simulator.Simulator;

public class CentralityTest {

    static HashMap<Integer, List<NodeDistance<Integer>>> networkLinks = new HashMap<>();

    static AdversaryKnowledge globalKnowledge;

    @BeforeAll
    public static void setupNetwork() {
        Simulator.reset();
        int networkSize = 6;
        for (int i = 0; i < networkSize; i++) {
            Node node = new Node(i + 1,  i, 0,
                    "simblock.node.routing.RandomGraphRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
        //          F ------+--------+
        //          | 2     | 1      | 4
        //      1   |    2  |    3   |
        // A ------ B ----- D ------ E
        //  \        \      |        |
        //   \4       \6    |3       |1
        //    \        \    |        |
        //     --- C -------+--------+
        List<Node> nodes = Simulator.getSimulatedNodes();
        networkLinks.put(0, Arrays.asList(
                new NodeDistance<Integer>(1, 1),
                new NodeDistance<Integer>(2, 4)));
        networkLinks.put(1, Arrays.asList(
                new NodeDistance<Integer>(0, 1),
                new NodeDistance<Integer>(2, 6),
                new NodeDistance<Integer>(3, 2),
                new NodeDistance<Integer>(5, 2)));
        networkLinks.put(2, Arrays.asList(
                new NodeDistance<Integer>(0, 4),
                new NodeDistance<Integer>(1, 6),
                new NodeDistance<Integer>(3, 3),
                new NodeDistance<Integer>(4, 1)));
        networkLinks.put(3, Arrays.asList(
                new NodeDistance<Integer>(1, 2),
                new NodeDistance<Integer>(2, 3),
                new NodeDistance<Integer>(4, 3),
                new NodeDistance<Integer>(5, 1)));
        networkLinks.put(4, Arrays.asList(
                new NodeDistance<Integer>(2, 1),
                new NodeDistance<Integer>(3, 3),
                new NodeDistance<Integer>(5, 4)));
        networkLinks.put(5, Arrays.asList(
                new NodeDistance<Integer>(1, 2),
                new NodeDistance<Integer>(3, 1),
                new NodeDistance<Integer>(4, 4)));

        for (Integer i : networkLinks.keySet()) {
            UndirectedRoutingTable rt = nodes.get(i).getRoutingTable();
            for (NodeDistance<Integer> nd : networkLinks.get(i)) {
                rt.addConnectionTo(nodes.get(nd.node));
            }
        }
    }

    @Test
    public void testComputeClosenessContrality() {
        try (MockedStatic<Network> mockedNetwork = mockStatic(Network.class)) {
            for (Integer i : networkLinks.keySet()) {
                for (NodeDistance<Integer> nd : networkLinks.get(i)) {
                    mockedNetwork.when(() -> Network.getEdgeLatency(i, nd.node)).thenReturn(Double.valueOf(nd.dist).longValue());
                }
            }
            // set the knowledge after the routing table is set.
            globalKnowledge = new AdversaryKnowledge(true);
            List<Node> nodes = globalKnowledge.knownNodes;

            ClosenessCentrality centrality = new ClosenessCentrality(globalKnowledge);
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(5));
            assertEquals(1.0/Double.MAX_VALUE, centrality.compute(nodes.get(0)));

            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3));
            assertEquals(1.0/8, centrality.compute(nodes.get(0)));

            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(5), nodes.get(4));
            assertEquals(1.0/9, centrality.compute(nodes.get(1)));

            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(1), nodes.get(3));
            assertEquals(1.0/8, centrality.compute(nodes.get(2)));

            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(5));
            assertEquals(1.0/6, centrality.compute(nodes.get(3)));

            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(2));
            assertEquals(1.0, centrality.compute(nodes.get(4)));

            globalKnowledge.restNodesForCentrality = new ArrayList<>();
            assertEquals(1.0/Double.MAX_VALUE, centrality.compute(nodes.get(5)));
        }
    }

    @Test
    public void testComputeDegreeCentrality() {
        List<Node> nodes = globalKnowledge.knownNodes;
        DegreeCentrality centrality = new DegreeCentrality(globalKnowledge);
        assertEquals(2, centrality.compute(nodes.get(0)));
        assertEquals(4, centrality.compute(nodes.get(1)));
        assertEquals(4, centrality.compute(nodes.get(2)));
        assertEquals(4, centrality.compute(nodes.get(3)));
        assertEquals(3, centrality.compute(nodes.get(4)));
        assertEquals(3, centrality.compute(nodes.get(5)));
    }

    @Test
    public void testComputeBetweennessCentrality() {
        try (MockedStatic<Network> mockedNetwork = mockStatic(Network.class)) {
            for (Integer i : networkLinks.keySet()) {
                for (NodeDistance<Integer> nd : networkLinks.get(i)) {
                    mockedNetwork.when(() -> Network.getEdgeLatency(i, nd.node)).thenReturn(Double.valueOf(nd.dist).longValue());
                }
            }
            // set the knowledge after the routing table is set.
            globalKnowledge = new AdversaryKnowledge(true);
            List<Node> nodes = globalKnowledge.knownNodes;
            BetweennessCentrality centrality = new BetweennessCentrality(globalKnowledge);

            // full network
            globalKnowledge.restNodesForCentrality = nodes;
            // A: B-C
            assertEquals(1, centrality.compute(nodes.get(0)));
            // B: A-D, A-F,
            assertEquals(2, centrality.compute(nodes.get(1)));
            // C: A-E,
            assertEquals(1, centrality.compute(nodes.get(2)));
            // D: B-C, C-F, B-E, E-F
            assertEquals(4, centrality.compute(nodes.get(3)));
            // E: 0
            assertEquals(0, centrality.compute(nodes.get(4)));
            // F: 0
            assertEquals(0, centrality.compute(nodes.get(5)));

            // without F
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4));
            // A: B-C
            assertEquals(1, centrality.compute(nodes.get(0)));
            // B: A-D,
            assertEquals(1, centrality.compute(nodes.get(1)));
            // C: A-E,
            assertEquals(1, centrality.compute(nodes.get(2)));
            // D: B-C, B-E
            assertEquals(2, centrality.compute(nodes.get(3)));
            // E: 0
            assertEquals(0, centrality.compute(nodes.get(4)));
            // F: 0
            assertEquals(0, centrality.compute(nodes.get(5)));

            // without B, C, D
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(4), nodes.get(5));
            // A: 0
            assertEquals(0, centrality.compute(nodes.get(0)));
            // E: 0
            assertEquals(0, centrality.compute(nodes.get(4)));
            // F: 0
            assertEquals(0, centrality.compute(nodes.get(5)));

            // without A, E, F
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3));
            // B: 0
            assertEquals(0, centrality.compute(nodes.get(1)));
            // C: 0
            assertEquals(0, centrality.compute(nodes.get(2)));
            // D: B-C,  
            assertEquals(1, centrality.compute(nodes.get(3)));

            // without B, D
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(2), nodes.get(4), nodes.get(5));
            // A:  0
            assertEquals(0, centrality.compute(nodes.get(0)));
            // C: A-E, A-F
            assertEquals(2, centrality.compute(nodes.get(2)));
            // E: A-F, C-F
            assertEquals(2, centrality.compute(nodes.get(4)));
            // F: 0
            assertEquals(0, centrality.compute(nodes.get(5)));

            // without A, B, F
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(2), nodes.get(3), nodes.get(4));
            // C: 0
            assertEquals(0, centrality.compute(nodes.get(2)));
            // D: 0
            assertEquals(0, centrality.compute(nodes.get(3)));
            // E: 0
            assertEquals(0, centrality.compute(nodes.get(4)));

            // without C
            globalKnowledge.restNodesForCentrality = Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(3), nodes.get(4), nodes.get(5));
            // A: 0
            assertEquals(0, centrality.compute(nodes.get(0)));
            // B: A-D, A-F, A-E
            assertEquals(3, centrality.compute(nodes.get(1)));
            // D: A-E, E-F, B-E
            assertEquals(3, centrality.compute(nodes.get(3)));
            // E: 0
            assertEquals(0, centrality.compute(nodes.get(4)));
            // F: 0
            assertEquals(0, centrality.compute(nodes.get(5)));
        }

    }

    
}
