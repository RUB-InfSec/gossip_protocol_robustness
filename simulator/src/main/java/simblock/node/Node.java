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

package simblock.node;

import static simblock.simulator.Simulator.debugLog;

import simblock.settings.SimulationConfiguration;
import simblock.simulator.Network;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.node.consensus.RumorSpreading;

import java.util.*;

import simblock.block.Block;
import simblock.block.Rumor;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.propagation.AbstractPropagationProtocol;
import simblock.node.routing.UndirectedRoutingTable;
import simblock.node.routing.BitcoinRoutingTable;
import simblock.task.*;
import simblock.block.RumorState;

/**
 * A class representing a node in the network.
 */
public class Node {

    /**
     * Unique node ID.
     */
    private final int nodeID;

    /**
     * Region assigned to the node.
     */
    private final int region;

    /**
     * Mining power assigned to the node.
     */
    private long miningPower;

    /**
     * A nodes routing table.
     */
    private UndirectedRoutingTable routingTable;

    /**
     * a nodes propagtion protocol
     */
    protected AbstractPropagationProtocol propagationProtocol;

    /**
     * The consensus algorithm used by the node.
     */
    private AbstractConsensusAlgo consensusAlgo;

    /**
     * The node causes churn.
     */
    public boolean isChurnNode;

    /**
     * The current block.
     */
    private Block block;

    /**
     * Orphaned blocks known to node.
     */
    private final Set<Block> orphans = new HashSet<>();

    /**
     * The current minting task
     */
    private AbstractMintingTask mintingTask = null;

    public boolean isMiningPool = false;

    /** Record state transition of each rumor on the node */
    public HashMap<Block, ArrayList<RumorState>> rumorStates = new HashMap<>();

    /** whether the node is corrupted */
    private boolean corrupted = false;

    /** whether the node is currently online */
    private boolean online = true;

    private float stakeProportion = 0.0f;

    /**
     * Cached per-node stake proportions derived from
     * {@link SimulationConfiguration#STAKE_PROPORTION_DISTRIBUTION}, keyed by
     * the network size they were computed for so they only get recomputed
     * when the network size changes.
     */
    private static float[] cachedStakeProportions = null;
    private static int cachedStakeProportionsNetworkSize = -1;

    /**
     * Derives the stakeProportion for a node out of a network of the given size,
     * by resampling {@link SimulationConfiguration#STAKE_PROPORTION_DISTRIBUTION}
     * (the real-world stake distribution, sorted descending, treated as a
     * fixed-length curve indexed by rank) to `networkSize` points via linear
     * interpolation along that curve, then renormalizing so the `networkSize`
     * values sum to 1.0. Interpolating -- rather than picking the nearest
     * source entry -- avoids runs of nodes collapsing onto the same source
     * value (most visibly onto the ~0 tail) once `networkSize` exceeds the
     * source array's length, so the shape (skew/concentration) of the source
     * distribution is preserved smoothly whether the network is smaller,
     * equal to, or much larger than the source sample.
     *
     * @param networkSize the total number of nodes in the network
     * @return an array of `networkSize` stake proportions summing to 1.0
     */
    private static float[] computeStakeProportions(int networkSize) {
        float[] distribution = SimulationConfiguration.STAKE_PROPORTION_DISTRIBUTION;
        int sourceSize = distribution.length;
        float[] proportions = new float[networkSize];
        float sum = 0.0f;
        for (int i = 0; i < networkSize; i++) {
            // Position of node i (0-indexed rank) along the source curve's rank
            // axis [0, sourceSize - 1], preserving relative rank regardless of
            // how networkSize compares to sourceSize.
            double pos = networkSize == 1 ? 0.0 : (double) i * (sourceSize - 1) / (networkSize - 1);
            int lower = (int) Math.floor(pos);
            int upper = Math.min(lower + 1, sourceSize - 1);
            double frac = pos - lower;
            float value = (float) (distribution[lower] * (1 - frac) + distribution[upper] * frac);
            proportions[i] = value;
            sum += value;
        }
        if (sum > 0) {
            for (int i = 0; i < networkSize; i++) {
                proportions[i] /= sum;
            }
        }
        return proportions;
    }

    /**
     * Instantiates a new Node.
     *
     * @param nodeID            the node id
     * @param region            the region
     * @param miningPower       the mining power
     * @param routingTableName  the routing table name
     * @param consensusAlgoName the consensus algorithm name
     * @param isChurnNode       whether the node causes churn
     */
    public Node(
            int nodeID, int region, long miningPower, String routingTableName,
            String consensusAlgoName, String propagationProtocol, boolean isChurnNode) {
        this.nodeID = nodeID;
        this.region = region;
        this.miningPower = miningPower;
        this.isChurnNode = isChurnNode;

        // Assign a stakeProportion based on the network size configured in Config,
        // so that all nodes' stakeProportion sums to 1 and follows the shape of
        // SimulationConfiguration.STAKE_PROPORTION_DISTRIBUTION.
        int networkSize = SimulationConfiguration.SIM_CONFIGS != null
                ? SimulationConfiguration.SIM_CONFIGS.network.size : 0;
        if (networkSize > 0 && nodeID >= 1 && nodeID <= networkSize) {
            if (cachedStakeProportions == null || cachedStakeProportionsNetworkSize != networkSize) {
                cachedStakeProportions = computeStakeProportions(networkSize);
                cachedStakeProportionsNetworkSize = networkSize;
            }
            this.stakeProportion = cachedStakeProportions[nodeID - 1];
        }
        try {
            this.routingTable = (UndirectedRoutingTable) Class.forName(routingTableName).getConstructor(
                    Node.class).newInstance(this);
            this.propagationProtocol = (AbstractPropagationProtocol) Class.forName(propagationProtocol)
                    .getConstructor(Node.class).newInstance(this);
            this.consensusAlgo = (AbstractConsensusAlgo) Class.forName(consensusAlgoName).getConstructor(
                    Node.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return String.format("Node %d", nodeID);
    }

    public boolean isCorrupted() {
        return corrupted;
    }

    public void setCorrupted() {
        corrupted = true;
    }

    public void resetCorrupted() {
        corrupted = false;
    }

    public void setOnline() {
        online = true;
        // Try to connect some new nodes as old connections may have dropped
        // them while they are offline.
        this.routingTable.reconnect2Network();
    }

    public void setOffline() {
        online = false;
    }

    public boolean isOnline() {
        return online;
    }

    public void scheduleOfflineEvent() {
        if (!isChurnNode) {
            return;
        }
        if (!online) {
            System.err.println("Error when schedule the next offline event: " + this + " currently already offline.");
            return;
        }
        int upTime = Network.getUpSessionInMilliSec();
//        debugLog(String.format("Node %d set up session time: %d s", nodeID, upTime/1000 ));
        Timer.putTask(new GoOfflineTask(this, upTime));
    }

    public void scheduleOnlineEvent() {
        if (!isChurnNode) {
            return;
        }
        if (online) {
            System.err.println("Error when schedule the next online event: " + this + " currently already online.");
            return;
        }
        int downTime = Network.getDownSessionInMilliSec();
//        debugLog(String.format("Node %d set down session time: %d s", nodeID, downTime/1000 ));
        Timer.putTask(new GoOnlineTask(this, downTime));
    }

    /**
     * Whether the node uses compact block relay.
     */
    protected boolean useCBR() {
        return (this.propagationProtocol.useCBR());
    }

    /**
     * Gets the node id.
     *
     * @return the node id
     */
    public int getNodeID() {
        return this.nodeID;
    }

    /**
     * Gets the region ID assigned to a node.
     *
     * @return the region
     */
    public int getRegion() {
        return this.region;
    }

    /**
     * Gets mining power.
     *
     * @return the mining power
     */
    public long getMiningPower() {
        return this.miningPower;
    }

    /**
     * sets mining power
     *
     * @param power mining power
     */
    public void setMiningPower(long power) {
        this.miningPower = power;
    }

    public AbstractPropagationProtocol getPropagationProtocol() {
        return this.propagationProtocol;
    }

    /** for test only */
    public void setPropagationProtocol(AbstractPropagationProtocol newProp) {
        this.propagationProtocol = newProp;
    }

    public float getStakeProportion() {
        return this.stakeProportion;
    }

    /** for test only */
    public void setStakeProportion(float sp) {
        this.stakeProportion = sp;
    }

    /**
     * Gets the consensus algorithm.
     *
     * @return the consensus algorithm. See {@link AbstractConsensusAlgo}
     */
    @SuppressWarnings("unused")
    public AbstractConsensusAlgo getConsensusAlgo() {
        return this.consensusAlgo;
    }

    /**
     * Gets routing table.
     *
     * @return the routing table
     */
    public UndirectedRoutingTable getRoutingTable() {
        return this.routingTable;
    }

    /**
     * Gets the current block.
     *
     * @return the block
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * Gets all orphans known to node.
     *
     * @return the orphans
     */
    public Set<Block> getOrphans() {
        return this.orphans;
    }

    public Set<Node> getNeighbors() {
        return this.routingTable.getNeighbors();
    }

    public Set<Node> getNeighbors(Set<Node> informedNodes) {
        return this.routingTable.getNeighbors(informedNodes);
    }

    /**
     * Initializes the routing table.
     */
    public void joinNetwork(boolean connectMiners) {
        if (this.routingTable instanceof BitcoinRoutingTable) {
            ((BitcoinRoutingTable) this.routingTable).setConnectToMiners(connectMiners);
        }
        this.routingTable.initTable();
    }

    public static void scheduleGenesisBlock(int delay) {
        if (SimulationConfiguration.PROPAGATION_CLASS.contains("Push")) {
            Rumor.endState = RumorState.EndState.ONE;
        } else {
            Rumor.endState = RumorState.EndState.TWO;
        }
        CreateRumorTask task = new CreateRumorTask(null, delay);
        Timer.putTask(task);

        if (SimulationConfiguration.PROPAGATION_CLASS.contains("Gossipsub")) {
            Timer.putTask(new GossipHeartbeatTask(delay));
//            Timer.putTask(new PeerScoreDecayTask(selfNode));
        }
    }

    public void reset() {
        this.block = null;
        this.orphans.clear();
        this.propagationProtocol.clear();
        this.routingTable.reset();
    }

    /**
     * Adds a new block to the to chain. If node was minting that task instance is
     * abandoned, and
     * the new block arrival is handled.
     *
     * @param newBlock the new block
     */
    public void addToChain(Block newBlock) {
        // If the node has been minting
        if (!(this.consensusAlgo instanceof RumorSpreading)) {
            if (this.mintingTask != null) {
                Timer.removeTask(this.mintingTask);
                this.mintingTask = null;
            }
        }
        // Update the current block
        if (block == null || newBlock.getHeight() > block.getHeight()) {
            this.block = newBlock;
        }
        // Observe and handle new block arrival
        Simulator.recordBlockArrived(newBlock, this);
    }

    /**
     * Add orphans.
     *
     * @param orphanBlock the orphan block
     * @param validBlock  the valid block
     */
    public void addOrphans(Block orphanBlock, Block validBlock) {
        if (this.consensusAlgo instanceof RumorSpreading) {
            debugLog("[WARNING] Rumor should NOT trigger addOrphans()");
        }
        if (orphanBlock != validBlock) {
            this.orphans.add(orphanBlock);
            this.orphans.remove(validBlock);
            if (validBlock == null || orphanBlock.getHeight() > validBlock.getHeight()) {
                this.addOrphans(orphanBlock.getParent(), validBlock);
            } else if (orphanBlock.getHeight() == validBlock.getHeight()) {
                this.addOrphans(orphanBlock.getParent(), validBlock.getParent());
            } else {
                this.addOrphans(orphanBlock, validBlock.getParent());
            }
        }
    }

    /**
     * Generates a new minting task and registers it
     */
    public void minting(Block newBlock) {
        AbstractMintingTask task = this.consensusAlgo.minting();
        this.mintingTask = task;

        if (task != null) {
            if (consensusAlgo instanceof RumorSpreading) {
                if (block == newBlock && block.getMinter() == this) {
                    Timer.putTask(task);
                }
            } else {    // mining competation
                if (Timer.checkMiningTask(task.getParent(), Timer.getCurrentTime() + task.getInterval())) {
                    Timer.addRemovableTask(task);
                } else {
                    // task = null;    // what's point of setting local var to null??
                    this.mintingTask = null;
                }
            }
        }
    }

    /**
     * Process the received block.
     *
     * @param block the block
     * @return true if the block is processed successfully and will be further
     *         propagated
     */
    public boolean processBlock(Block block) {
        if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
            if (this.block != null && !this.block.isOnSameChainAs(block)) {
                // If orphan mark orphan
                this.addOrphans(this.block, block);
            }
            // Else add to canonical chain
            this.addToChain(block);
            // Generates a new minting task
            this.minting(block);
            return true;
        } else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block)) {
            // If the block was not valid but was an unknown orphan and is not on the same
            // chain as the current block
            this.addOrphans(block, this.block);
            Simulator.recordBlockArrived(block, this);
            return false;
        }
        return false;
    }

    public boolean isBlockValid(Block block) {
        return !this.orphans.contains(block) &&
                (this.consensusAlgo.isReceivedBlockValid(block, this.block) || !block.isOnSameChainAs(this.block));
    }

    /**
     * Update the state of the rumor message. Must be called after #relay or #hop is updated.
     */
    public void updateRumorState(Block rumor, int newState) {
        if (!(rumor instanceof Rumor)) {
            return;
        }
        RumorState state = this.getRumorState(rumor);
        if (!this.rumorReachEndState(rumor) && newState <= Block.endState.getValue() && newState > state.getState()) {
//            debugLog(String.format("Update rumor-%d on Node %d from state %d to state %d", rumor.getId(), nodeID, state.getState(), newState));
            ArrayList<RumorState> states = this.rumorStates.getOrDefault(rumor, new ArrayList<RumorState>());
            states.add(new RumorState(newState, rumor.getHopCount(this), Timer.getCurrentTime() - rumor.getTime()));
            this.rumorStates.putIfAbsent(rumor, states);
        }
    }

    /**
     * Return the latest state of the rumor
     * @param rumor
     */
    private RumorState getRumorState(Block rumor) {
        ArrayList<RumorState> states = this.rumorStates.get(rumor);
        if (states == null) {
            return RumorState.NULL_STATE;
        }
        return states.get(states.size() - 1);
    }

    public ArrayList<RumorState> getAllRumorStates(Block rumor) {
        ArrayList<RumorState> res = this.rumorStates.get(rumor);
        if (res != null) {
            return new ArrayList<RumorState>(res);
        }
        return null;
    }

    public boolean rumorReachEndState(Block rumor) {
        return this.getRumorState(rumor) == RumorState.NULL_STATE ? false : this.getRumorState(rumor).getState() == rumor.endState.getValue();
    }


    /**
     * Receive message.
     *
     * @param message the message
     */
    public void receiveMessage(AbstractMessageTask message) {
        this.propagationProtocol.handleReceivedMessage(message);
    }
}
