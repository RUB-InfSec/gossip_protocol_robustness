package simblock.block;

import org.junit.jupiter.api.Test;

import simblock.node.Node;
import simblock.simulator.Simulator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;

public class AbstractMessageTest {

    @BeforeAll
    public static void setupNetwork() {
        Simulator.reset();
        int networkSize = 4;
        for (int i = 0; i < networkSize; i++) {
            Node node = new Node(i + 1, i, 0,
                    "simblock.node.routing.UndirectedRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
    }

    @Test
    public void testRelay() {
        List<Node> nodes = Simulator.getSimulatedNodes();

        Block block = new Block(null, nodes.get(0), 100, null);
        assertEquals(0, block.getRelayCount(nodes.get(0)));
        // has not arrived on a node cannot increase relay
        assertFalse(block.increaseRelay(nodes.get(1), nodes.get(0)));

        assertTrue(block.increaseRelay(nodes.get(0), nodes.get(1)));
        assertEquals(0, block.getRelayCount(nodes.get(0)));
        assertEquals(1, block.getRelayCount(nodes.get(1)));

        assertTrue(block.increaseRelay(nodes.get(0), nodes.get(2)));
        assertEquals(0, block.getRelayCount(nodes.get(0)));
        assertEquals(1, block.getRelayCount(nodes.get(2)));

        // already arrived on a node does not increase relay
        assertFalse(block.increaseRelay(nodes.get(1), nodes.get(2)));
        assertEquals(0, block.getRelayCount(nodes.get(0)));
        assertEquals(1, block.getRelayCount(nodes.get(2)));

        assertTrue(block.increaseRelay(nodes.get(1), nodes.get(3)));
        assertEquals(0, block.getRelayCount(nodes.get(0)));
        assertEquals(1, block.getRelayCount(nodes.get(1)));
        assertEquals(2, block.getRelayCount(nodes.get(3)));
    }

    @Test
    public void testHop() {
        List<Node> nodes = Simulator.getSimulatedNodes();
        Block block = new Block(null, nodes.get(0), 100, null);
        assertEquals(0, block.getHopCount(nodes.get(0)));
        // has not arrived on a node cannot increase hop
        assertFalse(block.increaseHop(nodes.get(1), nodes.get(2)));

        assertTrue(block.increaseHop(nodes.get(0), nodes.get(1)));
        assertEquals(0, block.getHopCount(nodes.get(0)));
        assertEquals(1, block.getHopCount(nodes.get(1)));

        // already arrived on a node keeps increase the hop count of dest
        assertTrue(block.increaseHop(nodes.get(0), nodes.get(1)));
        assertEquals(0, block.getHopCount(nodes.get(0)));
        assertEquals(2, block.getHopCount(nodes.get(1)));

        assertTrue(block.increaseHop(nodes.get(0), nodes.get(2)));
        assertEquals(0, block.getHopCount(nodes.get(0)));
        assertEquals(1, block.getHopCount(nodes.get(2)));

        // already arrived on a node keeps increase the hop count of dest
        assertTrue(block.increaseHop(nodes.get(1), nodes.get(2)));
        assertEquals(2, block.getHopCount(nodes.get(1)));
        assertEquals(2, block.getHopCount(nodes.get(2)));

        assertTrue(block.increaseHop(nodes.get(1), nodes.get(3)));
        assertEquals(2, block.getHopCount(nodes.get(1)));
        assertEquals(3, block.getHopCount(nodes.get(3)));

    }

    @Test
    public void testReachedNode() {
        List<Node> nodes = Simulator.getSimulatedNodes();
        Block block = new Block(null, nodes.get(0), 100, null);

        assertTrue(block.reachedNode(nodes.get(0)));
        assertFalse(block.reachedNode(nodes.get(1)));
        assertFalse(block.reachedNode(nodes.get(2)));
        assertFalse(block.reachedNode(nodes.get(3)));
        assertFalse(block.reachedAllHonestNodes());

        block.increaseRelay(nodes.get(0), nodes.get(1));
        assertTrue(block.reachedNode(nodes.get(0)));
        assertTrue(block.reachedNode(nodes.get(1)));
        assertFalse(block.reachedNode(nodes.get(2)));
        assertFalse(block.reachedNode(nodes.get(3)));
        assertFalse(block.reachedAllHonestNodes());

        block.increaseRelay(nodes.get(1), nodes.get(2));
        assertTrue(block.reachedNode(nodes.get(0)));
        assertTrue(block.reachedNode(nodes.get(1)));
        assertTrue(block.reachedNode(nodes.get(2)));
        assertFalse(block.reachedNode(nodes.get(3)));
        assertFalse(block.reachedAllHonestNodes());

        nodes.get(3).setCorrupted();
        assertTrue(block.reachedAllHonestNodes());

        block.increaseRelay(nodes.get(0), nodes.get(3));
        assertTrue(block.reachedNode(nodes.get(0)));
        assertTrue(block.reachedNode(nodes.get(1)));
        assertTrue(block.reachedNode(nodes.get(2)));
        assertTrue(block.reachedNode(nodes.get(3)));
        assertTrue(block.reachedAllHonestNodes());
    }
}
