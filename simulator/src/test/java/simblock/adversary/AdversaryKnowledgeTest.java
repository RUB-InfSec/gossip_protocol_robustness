package simblock.adversary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import simblock.block.AbstractMessage;
import simblock.block.Block;
import simblock.graph.NodeDistance;
import simblock.node.Node;
import simblock.node.routing.UndirectedRoutingTable;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.simulator.TimerTest;
import simblock.task.AbstractMessageTask;
import simblock.task.BlockMessageTask;

public class AdversaryKnowledgeTest {

    static HashMap<Integer, List<NodeDistance<Integer>>> networkLinks = new HashMap<>();

    static AdversaryKnowledge globalKnowledge;
    static AdversaryKnowledge localKnowledge;

    @BeforeAll
    public static void setupNetwork() {
        Simulator.reset();
        int networkSize = 6;
        for (int i = 0; i < networkSize; i++) {
            Node node = new Node(i + 1, i, 0,
                    "simblock.node.routing.RandomGraphRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
        globalKnowledge = new AdversaryKnowledge(true);
        localKnowledge = new AdversaryKnowledge(false);
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
    public void testGlobalGetNeighbors() {
        List<Node> nodes = globalKnowledge.knownNodes;
        globalKnowledge.corruptedNodes.clear();
        assertArrayEquals(
                getSortedNodeIDArray(1, 2),
                getSortedNodeIDArray(globalKnowledge.getConnections(nodes.get(0))));
        assertArrayEquals(
                getSortedNodeIDArray(0, 2, 3, 5),
                getSortedNodeIDArray(globalKnowledge.getConnections(nodes.get(1))));
    }

    @Test
    public void testLocalGetNeighbors() {
        List<Node> nodes = localKnowledge.knownNodes;
        localKnowledge.corruptedNodes.clear();
        Node target;

        target = nodes.get(0);
        // do not know neighbors of an honest node
        assertTrue(localKnowledge.getConnections(target).isEmpty());

        // know neighbors of a corrupted node
        localKnowledge.corruptedNodes.add(target);
        assertArrayEquals(
                getSortedNodeIDArray(1, 2),
                getSortedNodeIDArray(globalKnowledge.getConnections(nodes.get(0))));

        target = nodes.get(1);

        assertTrue(localKnowledge.getConnections(target).isEmpty());

        // do not know neighbors of an attacked node
        localKnowledge.nodeCorruptionWip.add(target);
        assertTrue(localKnowledge.getConnections(target).isEmpty());
    }

    @Test
    public void testGetUnreachedNodes() {
        List<Node> nodes = Simulator.getSimulatedNodes();
        List<Node> unreached = new ArrayList<>(Simulator.getSimulatedNodes());

        Block block = new Block(null, nodes.get(0), 100, null);

        // msg reached issuer node 0
        unreached.remove(nodes.get(0));
        assertArrayEquals(getSortedNodeIDArray(unreached),
                getSortedNodeIDArray(localKnowledge.getUnreachedNodes(block)));
        // msg reached node 1,2,3
        block.increaseRelay(nodes.get(0), nodes.get(1));
        unreached.remove(nodes.get(1));
        assertArrayEquals(getSortedNodeIDArray(unreached),
                getSortedNodeIDArray(localKnowledge.getUnreachedNodes(block)));
        block.increaseRelay(nodes.get(0), nodes.get(2));
        unreached.remove(nodes.get(2));
        assertArrayEquals(getSortedNodeIDArray(unreached),
                getSortedNodeIDArray(localKnowledge.getUnreachedNodes(block)));
        block.increaseRelay(nodes.get(1), nodes.get(3));
        unreached.remove(nodes.get(3));
        assertArrayEquals(getSortedNodeIDArray(unreached),
                getSortedNodeIDArray(localKnowledge.getUnreachedNodes(block)));
    }

    @Deprecated
    public void testGetNodes2Release() {
        List<Node> nodes = Simulator.getSimulatedNodes();
        Adversary adversary = AdversaryTest.getGlobalKnowledgeCapability2Adversary();
        AdversaryKnowledge knowledge = adversary.knowledge;

        Block block = new Block(null, nodes.get(0), 100, null);
        BlockMessageTask blockTask = new BlockMessageTask(nodes.get(0), nodes.get(1), block, 0);
        knowledge.addSeenMessage(blockTask);

        // no corrupted nodes release no nodes
        assertTrue(knowledge.getNodes2Release().isEmpty());

        // corrupt block issuer 0 make it possible to release
        adversary.attackNode(nodes.get(0));
        assertArrayEquals(getSortedNodeIDArray(0), getSortedNodeIDArray(knowledge.getNodes2Release()));

        Block block1 = new Block(null, nodes.get(1), 100, null);
        BlockMessageTask blockTask1 = new BlockMessageTask(nodes.get(1), nodes.get(2), block1, 0);
        knowledge.addSeenMessage(blockTask1);

        // with another active message, node 0 cannot be released now
        assertTrue(knowledge.getNodes2Release().isEmpty());
        // now new block1 arrived node 0, it can be released again
        block1.increaseRelay(nodes.get(1), nodes.get(0));
        assertArrayEquals(getSortedNodeIDArray(0), getSortedNodeIDArray(knowledge.getNodes2Release()));

        // now block arrives node 1, attack node 1, it can be released
        Timer.runTask();    // corrupt node 0
        adversary.attackNode(nodes.get(1));
        Timer.runTask();    // corrupt node 1
        block.increaseRelay(nodes.get(0), nodes.get(1));
        assertArrayEquals(getSortedNodeIDArray(0,1), getSortedNodeIDArray(knowledge.getNodes2Release()));
    }

    @Test
    public void testGlobalGetCandidateNodes() {
        TimerTest.prepareTaskQueue();   // see tasks scheduled in TimerTest
        // corruption delay = 0
        Map<AbstractMessageTask, Integer> pendingTasks = globalKnowledge.getPendingTasks(0);
        Map<Node, Map<AbstractMessage, Integer>> candidates = globalKnowledge.getCandidateNodes(pendingTasks);
        // node1
        assertEquals(1, candidates.get(TimerTest.node1).size());
        assertEquals(350, candidates.get(TimerTest.node1).get(TimerTest.tx));
        // node2
        assertEquals(1, candidates.get(TimerTest.node2).size());
        assertEquals(500, candidates.get(TimerTest.node2).get(TimerTest.block));
        // node3
        assertEquals(1, candidates.get(TimerTest.node3).size());
        assertEquals(200, candidates.get(TimerTest.node3).get(TimerTest.block));
        // node4
        assertEquals(1, candidates.get(TimerTest.node4).size());
        assertEquals(450, candidates.get(TimerTest.node4).get(TimerTest.shred));

        // corruption delay = 300
        pendingTasks = globalKnowledge.getPendingTasks(300);
        candidates = globalKnowledge.getCandidateNodes(pendingTasks);
        // node1
        assertEquals(1, candidates.get(TimerTest.node1).size());
        assertEquals(50, candidates.get(TimerTest.node1).get(TimerTest.tx));
        // node2
        assertEquals(1, candidates.get(TimerTest.node2).size());
        assertEquals(200, candidates.get(TimerTest.node2).get(TimerTest.block));
        // node3
        assertFalse(candidates.containsKey(TimerTest.node3));
        // node4
        assertEquals(1, candidates.get(TimerTest.node4).size());
        assertEquals(150, candidates.get(TimerTest.node4).get(TimerTest.shred));

        // deprecated: All nodes are re-evaluated, whether it is already attacked/corrupted.
        // attacked node not included in the candidates
        // AdversaryCapability capability = new AdversaryCapability(2, 2, 300);
        // Adversary adversary = new Adversary(capability, globalKnowledge, new NoneAdversaryStrategy());
        // adversary.attackNode(TimerTest.node1);
        // candidates = globalKnowledge.getCandidateNodes(pendingTasks);
        // assertFalse(candidates.containsKey(TimerTest.node1));
    }

    public int getNodeId(int index) {
        List<Node> nodes = Simulator.getSimulatedNodes();
        return nodes.get(index).getNodeID();
    }

    public static Object[] getSortedNodeIDArray(Integer... index) {
        return Arrays.asList(index).stream().map(i -> i+1).sorted().toArray();
    }

    public static Object[] getSortedNodeIDArray(Collection<Node> nodes) {
        return nodes.stream().map(Node::getNodeID).sorted().toArray();
    }
}
