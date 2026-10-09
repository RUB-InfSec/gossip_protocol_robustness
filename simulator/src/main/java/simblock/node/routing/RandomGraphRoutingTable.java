/*
 * Copyright 2019 Distributed Systems Group
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.node.routing;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import org.jgrapht.util.SupplierUtil;
import simblock.node.Node;
import simblock.settings.SimulationConfiguration;

import static simblock.simulator.Simulator.simRng;

/**
 * Routing table of an undirected random graph used by a {@link Node}.
 */
public class RandomGraphRoutingTable extends UndirectedRoutingTable {

    static final int MIN_DEGREE = 1;   // no isolated nodes when creating the graph

    static int TARGET_MEAN_DEGREE;
    public static Graph<Integer, DefaultEdge> graph = null;

    public static void generateRandomGraphNP(int n, double p) {
        if (graph == null) {    // init graph once
            // vertex are integers starting from 1 just like the node IDs
            Supplier<Integer> vertextSupplier = SupplierUtil.createIntegerSupplier(1);
            graph = new SimpleGraph<>(vertextSupplier, SupplierUtil.createDefaultEdgeSupplier(), false);
            GnpRandomGraphGenerator<Integer, DefaultEdge> generator = new GnpRandomGraphGenerator<>(n, p, simRng.nextLong());
            generator.generateGraph(graph);

            TARGET_MEAN_DEGREE = (int) Math.round(n * p);
        }
    }

    public static void generateRandomGraphNM(int n, int m) {
        if (graph == null) {    // init graph once
            // vertex are integers starting from 1 just like the node IDs
            Supplier<Integer> vertextSupplier = SupplierUtil.createIntegerSupplier(1);
            graph = new SimpleGraph<>(vertextSupplier, SupplierUtil.createDefaultEdgeSupplier(), false);
            GnmRandomGraphGenerator<Integer, DefaultEdge> generator = new GnmRandomGraphGenerator<>(n, m, simRng.nextLong());
            generator.generateGraph(graph);

            TARGET_MEAN_DEGREE = m/n;
        }
    }

    public RandomGraphRoutingTable(Node n) {
        super(n);
    }

    /**
     * Table initialization.
     */
    @Override
    public void initTable() {
        for (DefaultEdge edge : graph.outgoingEdgesOf(selfNode.getNodeID())) {
            this.addConnectionTo(allNodes.get(graph.getEdgeTarget(edge)-1));
        }
    }

    /**
     * Replace the offline nodes with a new connection.
     * @param connections the list of connections to update
     * @return the updated list of connections.
     */
    @Override
    public Set<Node> updateConnections(Set<Node> connections) {
        if (!SimulationConfiguration.CHURN_ON) {
            return connections;
        }
        // If any neighbors go offline, remove that connection and establish new connections
        Set<Node> updatedConn = new HashSet<>();
        for (Node n: connections) {
            if (!isConnectionAlive(n)) {
                this.removeConnection(n);
                Node newNode = allNodes.get(simRng.nextInt(allNodes.size()));
                int trials = 0;
                while (!this.addConnectionTo(newNode) && trials < 5) {
                    newNode = allNodes.get(simRng.nextInt(allNodes.size()));
                    trials++;
                }
                if (trials < 5) {
                    updatedConn.add(newNode);
                }
            } else {
                updatedConn.add(n);
            }
        }
        return updatedConn;
    }

    /**
     * When the node is online again, establish new random connections in the network.
     */
    @Override
    public void reconnect2Network() {
        this.neighbors.clear();
        float prob = (float) TARGET_MEAN_DEGREE / allNodes.size();
        for (Node node: allNodes) {
            if (simRng.nextDouble() < prob) {
                this.addConnectionTo(node);
            }
        }
        int min_degree = Math.min(allNodes.size()-1, MIN_DEGREE);
        findRandomNConnections(min_degree);
    }

    @Override
    public void reset() {
        super.reset();
        graph = null;
    }

}
