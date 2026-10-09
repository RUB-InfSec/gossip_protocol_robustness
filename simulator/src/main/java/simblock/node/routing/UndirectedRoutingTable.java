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
import java.util.List;
import java.util.Set;

import simblock.node.Link;
import simblock.node.Node;
import simblock.simulator.Simulator;

import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

/**
 * Routing table of an undirected graph used by a {@link Node}.
 */
public class UndirectedRoutingTable {
    protected Node selfNode;

    final List<Node> allNodes = Simulator.getSimulatedNodes();
    /** The size of routing table about all known nodes */
    protected int routingTableSize;
    protected HashSet<Node> knownNodes = new HashSet<>();

    // non-duplicated set
    protected HashSet<Node> neighbors = new HashSet<>();

    public UndirectedRoutingTable(Node n) {
        selfNode = n;
    }

    /**
     * Table initialization. Do nothing here.
     */
    public void initTable() {}

    /**
     * @return the number of neighbors
     */
    public int getDegree() {
        return this.neighbors.size();
    }

    /**
     * @return a copy of all connected nodes.
     */
    public Set<Node> getNeighbors() {
        return new HashSet<>(neighbors);
    }

    /**
     * Gets the node's neighbors for broadcast excluding the informed nodes.
     *
     * @return the neighbors
     */
    public Set<Node> getNeighbors(Set<Node> informedNodes) {
        Set<Node> receivers = new HashSet<>();
        for (Node n : neighbors) {
            if (!informedNodes.contains(n)) {
                receivers.add(n);
            }
        }
        return receivers;
    }

    public void reset() {
        this.neighbors.clear();
        this.knownNodes.clear();
    }

    /**
     * If there is still a connection from this node to the given node
     *
     * @param to target node
     * @return true if the target node is online and connected to this node
     */
    public boolean isConnectionAlive(Node to) {
        return to.getNeighbors().contains(selfNode) && to.isOnline();
    }

    /**
     * Return an updated set of connections. Only return the subset of
     * connections that are online.
     * @param connections set of neighbors to check
     * @return list of online neighbors in the given connections set
     */
    public Set<Node> updateConnections(Set<Node> connections) {
        Set<Node> updatedConn = new HashSet<>();
        for (Node node: connections) {
            if (node.isOnline()) {
                updatedConn.add(node);
            }
        }
        return updatedConn;
    }

    /**
     * Do nothing when reconnected. Keep the original connections
     */
    public void reconnect2Network() { }


    /**
     * Return a random node which is not the given node
     * @param n
     * @return
     */
    public Node pickRandomNodeExcept(Node n) {
        List<Node> allNodes = Simulator.getSimulatedNodes();
        int idx = allNodes.indexOf(n);
        while (true) {
            int rnd = simRng.nextInt(allNodes.size());
            if (rnd != idx) {
                return allNodes.get(rnd);
            }
        }
    }

    /**
     * Review the routing table of the neighbors, and drop those from the
     * connections who is offline or no longer has the current node in the
     * routing table.
     */
    public void updateConnectionStatus() {
        for (Node n : getNeighbors()) {
            if (!isConnectionAlive(n)) {
                debugLog("Drop out inactive connection " + new Link(n, selfNode));
                removeConnection(n);
            }
        }
    }

    /**
     * Attempt to connect to a node. If the node is already a neighbor or is
     * offline, then return false.
     *
     * @param to the node
     * @return true if a *new* connection is established with the `to` node.
     */
    public boolean addConnectionTo(Node to) {
        if (to != selfNode && !neighbors.contains(to) && to.isOnline()) {
//            debugLog(selfNode + " -> " + to);
            neighbors.add(to);
            knownNodes.add(to);
            to.getRoutingTable().addConnectionTo(selfNode);
            return true;
        }
        return false;
    }

    /**
     * Remove the neighbor from the list of neighbors.
     *
     * @param to the target of the connection to remove
     */
    public void removeConnection(Node to) {
        knownNodes.remove(to);
        if (!neighbors.contains(to)) {
            // to prevent infinite loop
            return;
        }
        neighbors.remove(to);
        to.getRoutingTable().removeConnection(selfNode);
    }

    public void findRandomNConnections(int n) {
        int numNewConnections = n - getDegree();
        int trials = 0;
        while (getDegree() < n && trials < 5*numNewConnections) {
            if (!this.addConnectionTo(allNodes.get(simRng.nextInt(allNodes.size())))) {
                trials++;
            }
        }
    }
}
