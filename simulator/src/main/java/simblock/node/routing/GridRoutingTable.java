package simblock.node.routing;


import simblock.node.Node;
import simblock.settings.SimulationConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

/**
 * Routing table of Grid topology for Polkadot.
 * See https://github.com/paritytech/polkadot/blob/master/node/network/protocol/src/grid_topology.rs
 * Grid topology does not really support churn
 *
 * Grid topology support implementation
 * The basic operation of the 2D grid topology is that:
 *   * A validator producing a message sends it to its row-neighbors and its column-neighbors
 *   * A validator receiving a message originating from one of its row-neighbors sends it to its
 *     column-neighbors
 *   * A validator receiving a message originating from one of its column-neighbors sends it to its
 *     row-neighbors
 *
 * This grid approach defines 2 unique paths for every validator to reach every other validator in
 * at most 2 hops.
 *
 * However, we also supplement this with some degree of random propagation:
 * every validator, upon seeing a message for the first time, propagates it to 8 random peers.
 * This inserts some redundancy in case the grid topology isn't working or is being attacked -
 * an adversary doesn't know which peers a validator will send to.
 * This is combined with the property that the adversary doesn't know which validators will elect
 * to check a block.
 */
public class GridRoutingTable extends UndirectedRoutingTable {
    ArrayList<Node> rowNeighbors = new ArrayList<>();
    ArrayList<Node> colNeighbors = new ArrayList<>();

    /** Number of additional random connections for each message propagation */
    public static int NUM_RANDOM_CONNECTIONS = SimulationConfiguration.RANDOM_CONNECTION;

    public GridRoutingTable(Node node) {
        super(node);
    }

    @Override
    public void initTable() {
        int pos = allNodes.indexOf(selfNode);
        int gridSize = (int) Math.ceil(Math.sqrt(allNodes.size()));
        int myRow = pos / gridSize;
        int myCol = pos % gridSize;
        for (int i = myRow * gridSize; i < Math.min(myRow* gridSize + gridSize, allNodes.size()); i++) {
            if (i != pos) {
                rowNeighbors.add(allNodes.get(i));
            }
        }
        for (int j = myCol; j < allNodes.size(); j += gridSize) {
            if (j != pos) {
                colNeighbors.add(allNodes.get(j));
            }
        }
    }

    @Override
    public Set<Node> getNeighbors() {
        Set<Node> neighbors = new HashSet<>();
        neighbors.addAll(rowNeighbors);
        neighbors.addAll(colNeighbors);
        return neighbors;
    }

    @Override
    public Set<Node> getNeighbors(Set<Node> informedNodes) {
        Set<Node> neighbors = new HashSet<>();

        // A node may only receive the message from itself or one other node
        assert informedNodes.size() == 1;

        Node from = new ArrayList<>(informedNodes).get(0);
        if (rowNeighbors.contains(from)) {
            neighbors.addAll(colNeighbors);
        } else if (colNeighbors.contains(from)) {
            neighbors.addAll(rowNeighbors);
        } else {    // generate the message or get the message from a random connection or
            neighbors = getNeighbors();
        }

        // add random connections
        for (int i = 0; i < Math.min(allNodes.size(), NUM_RANDOM_CONNECTIONS); i++) {
            neighbors.add(allNodes.get(simRng.nextInt(allNodes.size())));
        }
        return neighbors;
    }

}
