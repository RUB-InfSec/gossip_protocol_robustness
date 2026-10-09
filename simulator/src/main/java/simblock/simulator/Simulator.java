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

package simblock.simulator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import com.google.gson.*;

import simblock.block.Block;
import simblock.block.Rumor;
import simblock.block.RumorState;
import simblock.block.Shred;
import simblock.node.Node;
import simblock.settings.NetworkConfiguration;
import simblock.settings.SimulationConfiguration;
import simblock.task.*;

import static simblock.settings.SimulationConfiguration.*;

/**
 * The type Simulator is tasked with maintaining the list of simulated nodes and
 * managing the
 * block interval. It observes and manages the arrival of new blocks at the
 * simulation level.
 */
public class Simulator {

    /**
     * prng repeatable for simulation with the same seeds
     */
    public static Random simRng = new Random();

    /**
     * A list of nodes that will be used in a simulation.
     */
    private static final ArrayList<Node> simulatedNodes = new ArrayList<>();

    /**
     * Get simulated nodes list.
     *
     * @return the array list
     */
    public static List<Node> getSimulatedNodes() {
        return Collections.unmodifiableList(simulatedNodes);
    }

    public static void reset() {
        for (Node node: simulatedNodes) {
            node.reset();
        }
        simulatedNodes.clear();
        // reset stats
        propagationTimes.clear();
        propagationRelays.clear();
        observedBlocks.clear();
    }

    /**
     * Add node to the list of simulated nodes.
     *
     * @param node the node
     */
    public static void addNode(Node node) {
        simulatedNodes.add(node);
    }

    /**
     * Remove node from the list of simulated nodes.
     *
     * @param node the node
     */
    @SuppressWarnings("unused")
    public static void removeNode(Node node) {
        simulatedNodes.remove(node);
    }

    /**
     * A list of observed {@link Block} instances.
     */
    static final ArrayList<Block> observedBlocks = new ArrayList<>();

    static final HashMap<Block, Integer> blockLastArrivalTime = new HashMap<>();

    /**
     * Record a list of propagation delay corresponding to each block (index
     * corresponds to the block index in `observedBlocks`)
     */
    static ArrayList<HashMap<Integer, Integer>> propagationTimes = new ArrayList<>();

    static HashMap<Block, Integer> sentMessageSizeStats = new HashMap<>();
    static int overallInvBatchMsgSize;    // record the overall batch inv message size and add to all blocks in the end

    /** For gossipsub, record node scores as Node X evaluated on Node Y: <X, <Y, <list>> */
    private static long recordPeriodInMilliSec = 0;
    static HashMap<Node, HashMap<Node, ArrayList<Double>>> nodeScores = new HashMap<>();

    // Record on each SampleNetworkStatTask the number of online and honest nodes.
    public static ArrayList<Integer> statsNumOnlineHonestNodes = new ArrayList<>();

    public static int statsGossipsubPruneHonest = 0;
    public static int statsGossipsubPruneCorrupted = 0;
    public static double statsGossipsubFailGraftTime = -1;  // in second

    /**
     * Record a list of propagation times in terms of mining pool corresponding to
     * each block (index corresponds to the block index in `observedBlocks`)
     */
//    private static ArrayList<ArrayList<Integer>> propagationCountMP = new ArrayList<>();

    /**
     * Record a list of propagation relays arrived on different nodes corresponding to
     * each block (index corresponds to the block index in `observedBlocks`)
     * The hashmap is used to save the memory, count how many nodes have received a
     * block with a certain number of relays: numRelays -> #nodes
     */
    private static ArrayList<HashMap<Integer, Integer>> propagationRelays = new ArrayList<>();
    private static ArrayList<Double> accumulatedStakes = new ArrayList<>();

    static HashMap<String, Integer> taskMsgSizeMap;

    /**
     * Based on the type of message task, record the statics of total message size sent in the simulation.
     * @param task
     */
    public static void updateSentMessageStats(Task task) {
        if (taskMsgSizeMap == null) {
            taskMsgSizeMap = new HashMap<>() {{
                    put(BlockMessageTask.class.getSimpleName(), SimulationConfiguration.BLOCK_SIZE);
                    put(InvMessageTask.class.getSimpleName(), 36);
                    put(RecMessageTask.class.getSimpleName(), 36);
                }};
        }
        if (task instanceof AbstractMessageTask msgTask) {
            if (msgTask.getMessage() instanceof Block block) {
                Integer size = sentMessageSizeStats.getOrDefault(block, 0)
                        + taskMsgSizeMap.getOrDefault(task.getClass().getSimpleName(), 0);
                sentMessageSizeStats.put(block, size);
            }

            // gossipsub
            if (msgTask instanceof InvBatchMessageTask invBatchMsgTask) {   // getMessage() returns `null`
                overallInvBatchMsgSize += taskMsgSizeMap.getOrDefault(InvMessageTask.class.getSimpleName(), 0) * invBatchMsgTask.getBlocks().size();
            }

            // chunk: TODO to be tested
            if (msgTask.getMessage() instanceof Shred shred) {
                Block block = shred.block;
                Integer size = sentMessageSizeStats.getOrDefault(block, 0) + shred.size;
                sentMessageSizeStats.put(block, size);
            }
        }
    }

    public static void setScoreRecordPeriod(int periodInMilliSec) {
        recordPeriodInMilliSec = periodInMilliSec;
    }

    /** Update the score of node x evaluated on node y */
    public static void updateNodeScore(Node x, Node y, Double score) {
        if (SIM_CONFIGS.run.number != 11) {
            return;
        }
        HashMap<Node, ArrayList<Double>> scoreRecs = nodeScores.getOrDefault(x, new HashMap<>());
        ArrayList<Double> scores = scoreRecs.getOrDefault(y, new ArrayList<>());
        if (scores.size() < Timer.getCurrentTime()/ recordPeriodInMilliSec) {
            scores.add(score);
            scoreRecs.putIfAbsent(y, scores);
            nodeScores.putIfAbsent(x, scoreRecs);
        }
    }

    /*
     * Even for already processed block, record how long does the network still
     * propagating the block even when all nodes have received the block.
     */
    public static void updateBlkLastArrival(Block blk) {
        if (blockLastArrivalTime.getOrDefault(blk, 0) < Timer.getCurrentTime() - blk.getTime()) {
            blockLastArrivalTime.put(blk, Timer.getCurrentTime() - blk.getTime());
        }
    }
    /*
     * function updates the propagation statistic lists. Note that it must be
     * called according to the time sequence, at least for the same block, the
     * propagation time must increase.
     *
     * if the propagtion time of a block is within the approximation window of
     * the last recorded value, it will be regarded as the same and simply increase
     * the
     * propagationCount; otherwise it is added as a new record.
     */
    private static void updatePropagationStats(Block block, Node node) {
        int index = observedBlocks.indexOf(block);
        HashMap<Integer, Integer> relays = propagationRelays.get(index);
        HashMap<Integer, Integer> latencies = propagationTimes.get(index);

        int numRelays = block.getRelayCount(node);
        relays.put(numRelays, relays.getOrDefault(numRelays, 0) + 1);

        accumulatedStakes.set(index, accumulatedStakes.get(index) + node.getStakeProportion());

        int propagation_time = Timer.getCurrentTime() - block.getTime();
        int lastLatency = Collections.max(latencies.keySet());
        if (NetworkConfiguration.SIMULATE_REAL_NETWORK_LATENCY) {
            int approxWindow = propagation_time < APPROX_CUTOFF ? APPROX_WIN1 : APPROX_WIN2;
            if (propagation_time - lastLatency < approxWindow) {
                latencies.put(lastLatency, latencies.get(lastLatency) + 1);
            } else {
                latencies.put(propagation_time, 1);
            }
        } else {
            if (propagation_time == lastLatency) {
                latencies.put(lastLatency, latencies.get(lastLatency) + 1);
            } else {
                latencies.put(propagation_time, 1);
            }
        }
    }

    /**
     * Handle the arrival of a new block. For every observed block, propagation
     * information is updated, and for a new block propagation information is
     * created.
     *
     * @param block the block
     * @param node  the node who just observed the block
     */
    public static void recordBlockArrived(Block block, Node node) {
        if (node.isCorrupted()) {
            return;
//            throw new RuntimeException("Corrupted node record block arrival!");
        }
        debugLog(String.format("Node %s: block H=%d arrived: ", node, block.getHeight()) + block);
        // If block is already seen by any node
        if (observedBlocks.contains(block)) {
            updatePropagationStats(block, node);
        } else {
            // initialize stats: the block has not been seen by any node, just mined on the
            // minter.
            observedBlocks.add(block);
            propagationTimes.add(new HashMap<>() {{ put(0, 1);}});
            propagationRelays.add(new HashMap<>() {{ put(0, 1);}}); // only one node (the issuer) has 0 relay
            accumulatedStakes.add(0.0);
        }
    }

    /**
     * Print propagation information about the propagation of the provided block in
     * the format:
     *
     * <p>
     * <em> propagation_time,count, mining power </em>
     * <p>
     * <em>propagation_time</em>: The time from when the block of the block ID is
     * generated to
     * when the
     * <p>
     * <em>count</em>: number of arraivals within delta of propagation time
     * <p>
     * <em>mining power</em>:sum of miningpower of all nodes that recieved the block
     * at this time</em>
     *
     *
     * @param block the block
     */
    private static void printPropagation(PrintWriter pw, Block block,
            HashMap<Integer, Integer> relays, HashMap<Integer, Integer> latencies, double accumulatedStake) {
//            ArrayList<Integer> timeCntMP) {

        Gson gson = new Gson();

        JsonObject blockStat = new JsonObject();
        blockStat.add("relays", gson.toJsonTree(relays));
        blockStat.add("latency", gson.toJsonTree(latencies));
        blockStat.addProperty("accumulatedStake", accumulatedStake);

        if (block instanceof Rumor rumor && SimulationConfiguration.RECORD_STATE_TRANSIT) {
            JsonArray gStates = new JsonArray();
            for (Node node: simulatedNodes) {
                ArrayList<RumorState> states = node.getAllRumorStates(rumor);
                if (states != null) {
                    for (RumorState s : states) {
                        gStates.add(gson.toJsonTree(new int[] {s.getState(), s.getHops(), s.getTime()}));
                    }
                }
            }
            blockStat.add("stateTransits", gStates);
        }
        blockStat.addProperty("sentMessageSize", sentMessageSizeStats.getOrDefault(block, 0));
        blockStat.addProperty("blkStayTime", blockLastArrivalTime.getOrDefault(block, 0));
        pw.printf("\"%s\":%s",block.toString(), blockStat.toString());
    }

    /**
     * Print propagation information about all blocks, internally relying on
     * {@link Simulator#printPropagation(Block, LinkedHashMap)}.
     */
    public static void printStats(PrintWriter pw) {
        int avgInvBatchSize = overallInvBatchMsgSize / observedBlocks.size();

        boolean first = true;
        pw.print("{");
        for (int i = 0; i < observedBlocks.size(); i++) {
            if (!first) {
                pw.println(",");
            }
            Block block = observedBlocks.get(i);
            sentMessageSizeStats.put(block, sentMessageSizeStats.getOrDefault(block, 0) + avgInvBatchSize);
            printPropagation(pw, block,
                    propagationRelays.get(i),
                    propagationTimes.get(i),
                    accumulatedStakes.get(i));
            first = false;
        }
        pw.printf(Locale.ROOT, ",\"numAvgOnlineHonestNodes\":%f", statsNumOnlineHonestNodes.stream().mapToInt(Integer::intValue).average().orElse(0));
        if ( ROUTING_TABLE.toUpperCase().contains("GOSSIPSUB")) {
            pw.printf(",\"gossipsub\": {");
            pw.printf("\"numPrunedHonestNodes\":%d", statsGossipsubPruneHonest);
            pw.printf(",\"numPrunedCorruptedNodes\":%d", statsGossipsubPruneCorrupted);
            pw.printf(Locale.ROOT, ",\"failedGraftTime\":%f", statsGossipsubFailGraftTime);
            List<Node> honestNodes = Simulator.getSimulatedNodes().stream().filter(n -> !n.isCorrupted()).toList();
            pw.printf(Locale.ROOT, ",\"honestNodeDegree\":%f", 1.0 * honestNodes.stream().mapToInt(n -> n.getNeighbors().size()).sum() / honestNodes.size());
            pw.print("}");
        }
        pw.print("}");
        pw.flush();
        pw.close();
    }

    public static void printGossipScoreStats(PrintWriter pw) {
        Gson gson = new Gson();
        JsonObject nodeScoreJ = new JsonObject();
        for (Node n: nodeScores.keySet()) {
            nodeScoreJ.add(n.toString(), gson.toJsonTree(nodeScores.get(n)));
        }
        pw.printf(nodeScoreJ.toString());
        pw.flush();
        pw.close();
    }

    public static void debugLog(String message) {
        if (DEBUG_MODE) {
            System.out.printf("[debug](t=%,d) %s\n", Timer.getCurrentTime(), message);
        }
    }
}
