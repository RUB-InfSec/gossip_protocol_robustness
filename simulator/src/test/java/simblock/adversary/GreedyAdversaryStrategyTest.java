package simblock.adversary;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import simblock.block.AbstractMessage;
import simblock.block.Block;
import simblock.graph.centrality.ClosenessCentrality;
import simblock.node.Node;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Simulator;

public class GreedyAdversaryStrategyTest {

    @BeforeAll
    public static void setupNetwork() {
        Simulator.reset();
        int networkSize = 6;
        for (int i = 0; i < networkSize; i++) {
            Node node = new Node(i + 1, 1, 0,
                    "simblock.node.routing.RandomGraphRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
    }

    @Test
    public void testRankNodesClosenessCentrality() {
        SimulationConfiguration.CENTRALITY_CLASS = "simblock.graph.centrality.ClosenessCentrality";
        List<Node> nodes = Simulator.getSimulatedNodes();
        Map<Node, Map<AbstractMessage, Integer>> candidates = new HashMap<>();
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            blocks.add(new Block(null, nodes.get(i), 0, null));
        }
        AdversaryKnowledge knowledge = new AdversaryKnowledge(true);
        prepareTestCandidates(blocks, candidates);
        // mock ClosenessCentrality.compute
        ClosenessCentrality spyCloseCentrality = spy(new ClosenessCentrality(knowledge));
        List<List<Node>> unreachedNodes = new ArrayList<>() {};
        for (int i = 0; i < blocks.size(); i++) {
            unreachedNodes.add(knowledge.getUnreachedNodes(blocks.get(i)));
        }
        double[][] mockedCentralities = new double[][] {
            {50,  130, 100, 70,  80,  90},
            {80,  90,  120, 70,  90,  80},
            {70,  120, 130, 80,  100, 90},
            {150, 60,  90,  110, 80,  40},
            {100, 80,  60,  50,  70,  60},
            {90,  150, 120, 70,  100, 80}
        };
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < blocks.size(); j++) {
                knowledge.restNodesForCentrality = unreachedNodes.get(j);
                doReturn(mockedCentralities[i][j]).when(spyCloseCentrality).compute(nodes.get(i));
            }
        }
        // node 0 closenessCentralities: 580
        // block1 -> 130 + 50 = 180
        // block3 -> 70 + 150 = 220
        // block4 -> 80 + 100 = 180
        // node 1: 300
        // block2 -> 120 + 10 = 130
        // block4 -> 90 + 80 = 170
        // node 2: 460
        // block1 -> 120 + 40 = 160
        // block3 -> 80 + 70 = 150
        // block5 -> 90 + 60 = 150
        // node 4: 560
        // block1 -> 80 + 200 = 280
        // block3 -> 50 + 120 = 170
        // block5 -> 60 + 50 = 110
        // node 5
        // block2 -> 120 + 30 = 150

        // node 2 > node 4 > node 0 > node 1 > node 5
        GreedyAdversaryStrategy greedyStrategy = new GreedyAdversaryStrategy(knowledge);
        Node[] result = new Node[5];
        assertArrayEquals(
            new Node[]{nodes.get(2), nodes.get(4), nodes.get(0), nodes.get(1), nodes.get(5)},
            greedyStrategy.rankNodesClosenessCentrality(candidates).toArray(result));
    }

    void prepareTestCandidates(List<Block> blocks, Map<Node, Map<AbstractMessage, Integer>> candidates) {
        // each node generate a block
        List<Node> nodes = Simulator.getSimulatedNodes();
        // block 0 arrives 0,1,2,3,4,5
        // block 1 arrives 1,3,5
        // block 2 arrives 0,2,3,4
        // block 3 arrives 1,3
        // block 4 arrives 3,4,5
        // block 5 arrives 5
        blocks.get(0).increaseRelay(nodes.get(0), nodes.get(1));
        blocks.get(0).increaseRelay(nodes.get(0), nodes.get(2));
        blocks.get(0).increaseRelay(nodes.get(1), nodes.get(3));
        blocks.get(0).increaseRelay(nodes.get(2), nodes.get(4));
        blocks.get(0).increaseRelay(nodes.get(4), nodes.get(5));
        blocks.get(1).increaseRelay(nodes.get(1), nodes.get(3));
        blocks.get(1).increaseRelay(nodes.get(1), nodes.get(5));
        blocks.get(2).increaseRelay(nodes.get(2), nodes.get(0));
        blocks.get(2).increaseRelay(nodes.get(2), nodes.get(3));
        blocks.get(2).increaseRelay(nodes.get(0), nodes.get(4));
        blocks.get(3).increaseRelay(nodes.get(4), nodes.get(1));
        blocks.get(4).increaseRelay(nodes.get(1), nodes.get(3));
        blocks.get(4).increaseRelay(nodes.get(4), nodes.get(5));
        // nodes expect blocks to arrive but some may not be scheduled yet (in parenthese)
        // node 0 expect block 1,3,4 (5)
        // node 1 expect block 2,4,(5)
        // node 2 expect block 1,3,(4),5
        // node 3 expect block (5)
        // node 4 expect block 1,3,5
        // node 5 expect block 2,(3)
        Map<AbstractMessage, Integer> scheduledMsgs;
        scheduledMsgs = new HashMap<AbstractMessage, Integer>();
        candidates.put(nodes.get(0), scheduledMsgs);
        scheduledMsgs.put(blocks.get(1), 50);
        scheduledMsgs.put(blocks.get(3), 150);
        scheduledMsgs.put(blocks.get(4), 100);
        scheduledMsgs = new HashMap<AbstractMessage, Integer>();
        candidates.put(nodes.get(1), scheduledMsgs);
        scheduledMsgs.put(blocks.get(2), 10);
        scheduledMsgs.put(blocks.get(4), 80);
        scheduledMsgs = new HashMap<AbstractMessage, Integer>();
        candidates.put(nodes.get(2), scheduledMsgs);
        scheduledMsgs.put(blocks.get(1), 40);
        scheduledMsgs.put(blocks.get(3), 70);
        scheduledMsgs.put(blocks.get(4), 60);
        scheduledMsgs = new HashMap<AbstractMessage, Integer>();
        candidates.put(nodes.get(4), scheduledMsgs);
        scheduledMsgs.put(blocks.get(1), 200);
        scheduledMsgs.put(blocks.get(3), 120);
        scheduledMsgs.put(blocks.get(5), 50);
        scheduledMsgs = new HashMap<AbstractMessage, Integer>();
        candidates.put(nodes.get(5), scheduledMsgs);
        scheduledMsgs.put(blocks.get(2), 30);
    }

    @Test
    public void testObliviousFindTargets() {
        AdversaryKnowledge spyGlobalKnowledge = spy(new AdversaryKnowledge(true));
        GreedyAdversaryStrategy greedyStrategy = new GreedyAdversaryStrategy(spyGlobalKnowledge);

        // mock iterative centrality computation
        ClosenessCentrality spyCloseCentrality = spy(new ClosenessCentrality(spyGlobalKnowledge));
        double[] mockedCentralityInverses = new double[] {50,  130, 100, 70,  80,  90};
        List<Node> nodes = Simulator.getSimulatedNodes();
        List<Node> rest;
        // each node to the rest of the nodes
        for (Node node : nodes) {
            List<Node> others = new ArrayList<>(nodes);
            others.remove(node);
            spyGlobalKnowledge.restNodesForCentrality = others;
            doReturn(mockedCentralityInverses[nodes.indexOf(node)]).when(spyCloseCentrality).compute(node);
        }
        // except node 0 (1)
        mockedCentralityInverses = new double[] {90, 80, 64, 95, 40};
        rest = Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4), nodes.get(5));
        for (Node node : rest) {
            List<Node> others = new ArrayList<>(rest);
            others.remove(node);
            spyGlobalKnowledge.restNodesForCentrality = others;
            doReturn(mockedCentralityInverses[rest.indexOf(node)]).when(spyCloseCentrality).compute(node);
        }
        // except node 5 (6)
        mockedCentralityInverses = new double[] {70, 30, 50, 80};
        rest = Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4));
        for (Node node : rest) {
            List<Node> others = new ArrayList<>(rest);
            others.remove(node);
            spyGlobalKnowledge.restNodesForCentrality = others;
            doReturn(mockedCentralityInverses[rest.indexOf(node)]).when(spyCloseCentrality).compute(node);
        }
        // except node 2 (3)
        mockedCentralityInverses = new double[] {80, 100, 70};
        rest = Arrays.asList(nodes.get(1), nodes.get(3), nodes.get(4));
        for (Node node : rest) {
            List<Node> others = new ArrayList<>(rest);
            others.remove(node);
            spyGlobalKnowledge.restNodesForCentrality = others;
            doReturn(mockedCentralityInverses[rest.indexOf(node)]).when(spyCloseCentrality).compute(node);
        }
        // except node 4 (5)
        mockedCentralityInverses = new double[] {50, 60};
        rest = Arrays.asList(nodes.get(1), nodes.get(3));
        for (Node node : rest) {
            List<Node> others = new ArrayList<>(rest);
            others.remove(node);
            spyGlobalKnowledge.restNodesForCentrality = others;
            doReturn(mockedCentralityInverses[rest.indexOf(node)]).when(spyCloseCentrality).compute(node);
        }
        // mock connection
        doReturn(Arrays.asList(nodes.get(1), nodes.get(4))).when(spyGlobalKnowledge).getConnections(nodes.get(0));
        doReturn(Arrays.asList(nodes.get(3), nodes.get(5), nodes.get(2))).when(spyGlobalKnowledge).getConnections(nodes.get(1));
        doReturn(Arrays.asList(nodes.get(0), nodes.get(3))).when(spyGlobalKnowledge).getConnections(nodes.get(2));
        doReturn(Arrays.asList(nodes.get(4), nodes.get(5))).when(spyGlobalKnowledge).getConnections(nodes.get(3));
        doReturn(Arrays.asList(nodes.get(0), nodes.get(3))).when(spyGlobalKnowledge).getConnections(nodes.get(4));
        doReturn(Arrays.asList(nodes.get(1), nodes.get(4))).when(spyGlobalKnowledge).getConnections(nodes.get(5));
        // ranked targeted nodes [0, 5, 2, 4, 1, 3], no more links as all nodes are targets
        AdversaryCapability capability = new AdversaryCapability(6, 0, 0);
        assertArrayEquals(new Node[] {nodes.get(0), nodes.get(5), nodes.get(2), nodes.get(4), nodes.get(1), nodes.get(3)},
            greedyStrategy.findTargets(capability).toArray());

        // limit capability to 2 nodes, and add 3 links
        capability = new AdversaryCapability(2, 0, 0);
        assertArrayEquals(new Object[] {nodes.get(0), nodes.get(5)}, 
            // new Link(nodes.get(3), nodes.get(2)), new Link(nodes.get(3), nodes.get(4)), new Link(nodes.get(3), nodes.get(1))},
            greedyStrategy.findTargets(capability).toArray());

        // deprecated: there is no influence as all nodes are reevaluated
        // now set node 0 already attacked, 
        Adversary adversary = new Adversary(capability, spyGlobalKnowledge, greedyStrategy);
        adversary.attackNode(nodes.get(0));
        assertArrayEquals(new Object[] {nodes.get(0), nodes.get(5)}, 
            // new Link(nodes.get(3), nodes.get(2)), new Link(nodes.get(3), nodes.get(4)), new Link(nodes.get(3), nodes.get(1))},
            greedyStrategy.findTargets(capability).toArray());
        // assertArrayEquals(new Object[] {nodes.get(3),
        //     new Link(nodes.get(3), nodes.get(4)), new Link(nodes.get(1), nodes.get(5)), new Link(nodes.get(4), nodes.get(5))},
        //     greedyStrategy.findTargets(spyGlobalKnowledge, capability).toArray());
    }

    @Test
    public void testAdaptiveFindTargets() {

    }
}
