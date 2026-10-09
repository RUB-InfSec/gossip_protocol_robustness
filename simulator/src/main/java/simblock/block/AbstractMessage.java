/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.block;

import simblock.node.Node;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Simulator;

import static simblock.simulator.Simulator.debugLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractMessage {

    /**
     * Block unique id, incremented upon each new object creation.
     */
    protected int id;

    /**
     * Record the number of node relays it takes for the message to reach a node
     */
    protected HashMap<Node, Integer> relays;
    private boolean relaysInitialized = false;
    /**
     * Hops count the number of communication rounds it takes for a message to reach a node.
     * For example, with advertisement-based push, it takes one relay but three hops for a msg to be pushed to a neighbor.
     */
    protected HashMap<Node, Integer> hops;

    public static RumorState.EndState endState;

    /**
     * Gets the block id.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    public String getMessageName() {
        return String.format("%s-%d", this.getClass().getSimpleName(), getId());
    }

    /**
     * Increment the relay count on the `dest` node when a message is
     * successfully propagated from the `src` node to the `dest` node. It also
     * updates the hop count (#message rounds used to relay the message)
     * @param src
     * @param dest
     *
     * @return true if increase the relay successfully
     */
    public boolean increaseRelay(Node src, Node dest) {
        if (SimulationConfiguration.PROPAGATION_CLASS.contains("Chunk")) {
            // The sender of the last Shred may not have reconstructed the original message yet.
            relays.put(dest, 1);
            return true;
        }
        if (!relays.containsKey(src)) {
            System.out.printf("[ERROR] %s hasn't arrived node %s, cannot increase relay.\n", getMessageName(), src);
            return false;
        }
        if (relays.containsKey(dest)) {
            System.out.printf("[WARNING] %s has already arrived node %s, ignore increase relay.\n", getMessageName(), dest);
            return false;
        }
//        debugLog(String.format("Node %s received %s after %d relays.", dest, getMessageName(), relays.get(src) + 1));
        relays.put(dest, relays.get(src) + 1);

        increaseHop(src, dest);
        return true;
    }

    /**
     * Increment the hop count on the `dest` node which counts the #rounds used
     * to relay the message from `src` to `dest`.
     * @param src   node that relays the message (not the intermediate message sender)
     * @param dest  node that receives the relayed message (not the intermediate message receiver)
     */
    public boolean increaseHop(Node src, Node dest) {
        if (!hops.containsKey(src)) {
            System.out.printf("[ERROR] %s hasn't arrived node %s, cannot increase hop.\n", getMessageName(), src);
            return false;
        }
        if (!hops.containsKey(dest)) {
            hops.put(dest, hops.get(src) + 1);
        } else {
            hops.put(dest, hops.get(dest) + 1);
        }
//        debugLog(String.format("Node %s increase hop (%d) on %s.", dest, getHopCount(dest), getMessageName()));
        return true;
    }

    protected void initRelay(Node issuer) {
        if (relaysInitialized) {
            System.out.printf("[WARNING] %s relay count already initialized.\n", getMessageName());
            return;
        }
        relaysInitialized = true;
        relays = new HashMap<>();
        relays.put(issuer, 0);
        hops = new HashMap<>();
        hops.put(issuer, 0);
        debugLog(String.format("Node %s issues %s.", issuer, getMessageName()));
    }

    public int getRelayCount(Node node) {
        if (!relays.containsKey(node)) {
            System.out.printf("[WARNING] %s has never reached node %s.\n", getMessageName(), node);
            return -1;
        }
        return relays.get(node);
    }

    /**
     * Return true if a nodes received this message
     * @param node
     * @return
     */
    public boolean reachedNode(Node node) {
        return relays.containsKey(node);
    }

    public boolean reachedAllHonestNodes() {
        List<Node> remainingNodes = new ArrayList<>(Simulator.getSimulatedNodes());
        remainingNodes.removeAll(relays.keySet());
        for (Node n : remainingNodes) {
            if (!n.isCorrupted()) {
                return false;
            }
        }
        return true;
    }

    public int getHopCount(Node node) {
        if (!hops.containsKey(node)) {
            System.out.printf("[WARNING] Node %s does not know the existence of %s.\n", node, getMessageName());
            return -1;
        }
        return hops.get(node);
    }
};
