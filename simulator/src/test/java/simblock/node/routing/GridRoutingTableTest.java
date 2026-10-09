package simblock.node.routing;

import org.junit.jupiter.api.Test;
import simblock.node.Node;
import simblock.simulator.Simulator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GridRoutingTableTest {

    List<Node> createNodes(int num) {
        Simulator.reset();
        for (int i = 0; i < num; i++) {
            Node node = new Node(i + 1, i, 0,
                    "simblock.node.routing.GridRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
        for (Node n: Simulator.getSimulatedNodes()) {
            n.joinNetwork(false);
        }
        return Simulator.getSimulatedNodes();
    }

    @Test
    public void testGetNeighbors() {
        /*
          0  1  2  3
          4  5  6  7
          8  9  10
         */
        List<Node> nodes = createNodes(11);

        Set<Node> neighbors;
        // Node 0
        neighbors = nodes.get(0).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4), nodes.get(8))));
        // Node 1
        neighbors = nodes.get(1).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(0), nodes.get(2), nodes.get(3), nodes.get(5), nodes.get(9))));
        // Node 2
        neighbors = nodes.get(2).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(0), nodes.get(3), nodes.get(6), nodes.get(10))));
        // Node 3
        neighbors = nodes.get(3).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(0), nodes.get(7))));
        // Node 4
        neighbors = nodes.get(4).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(0), nodes.get(5), nodes.get(6), nodes.get(7), nodes.get(8))));
        // Node 5
        neighbors = nodes.get(5).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(4), nodes.get(1), nodes.get(6), nodes.get(7), nodes.get(9))));
        // Node 6
        neighbors = nodes.get(6).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(4), nodes.get(5), nodes.get(2), nodes.get(7), nodes.get(10))));
        // Node 7
        neighbors = nodes.get(7).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(4), nodes.get(5), nodes.get(6), nodes.get(3))));
        // Node 8
        neighbors = nodes.get(8).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(0), nodes.get(4), nodes.get(9), nodes.get(10))));
        // Node 9
        neighbors = nodes.get(9).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(5), nodes.get(8), nodes.get(10))));
        // Node 10
        neighbors = nodes.get(10).getNeighbors();
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(8), nodes.get(9), nodes.get(2), nodes.get(6))));
    }

    @Test
    void testGetNeighborsFrom() {
        /*
          0  1  2  3
          4  5  6  7
          8  9  10
         */
        List<Node> nodes = createNodes(11);

        GridRoutingTable.NUM_RANDOM_CONNECTIONS = 0;
        Set<Node> neighbors;
        // Node 0, generate message
        neighbors = nodes.get(0).getNeighbors(new HashSet<>(Arrays.asList(nodes.get(0))));
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(3), nodes.get(4), nodes.get(8))));
        // Node 1, from row member
        neighbors = nodes.get(1).getNeighbors(new HashSet<>(Arrays.asList(nodes.get(0))));
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(5), nodes.get(9))));
        // Node 2, from col member
        neighbors = nodes.get(2).getNeighbors(new HashSet<>(Arrays.asList(nodes.get(6))));
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(0), nodes.get(3))));
        // Node 3, from random member
        neighbors = nodes.get(3).getNeighbors(new HashSet<>(Arrays.asList(nodes.get(10))));
        assertEquals(neighbors, new HashSet<>(Arrays.asList(nodes.get(1), nodes.get(2), nodes.get(0), nodes.get(7))));
    }
}
