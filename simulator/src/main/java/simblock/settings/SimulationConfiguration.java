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

package simblock.settings;

/**
 * The type Simulation configuration allows for specific simulation instance
 * configuration.
 */
public class SimulationConfiguration {

    public static class BlockchainConfig {
        /* blockchain network to simulate */
        public final String name;

        /* Gossip protocol name corresponding to the propagation class */
        public String propagationProtocolName;

        /* in byte */
        public final int blockSize;

        /* in byte */
        public final int transactionSize;
        /*
         * bitcoin cbr size based on average nr of transaction per block between
         * 23.10-30.10.23(blockchain.info) * 6 ( reduced transaction header size)
         * + 80 ( blockheader) + 100 (coinbase tx)
         * monero cbr size avg nr of tx (https://localmonero.co/blocks/stats) * 32 txid
         * size + header 46 + coinbase 100
         */
        public final int CBRblockSize;

        /* in millisecond */
        public final int blockInterval;

        /**
         * Max time in milliseconds to wait for a response after requesting a block
         * btc: https://github.com/bitcoin/bitcoin/blob/master/src/net_processing.cpp
         * l.126
         * eth:
         * https://github.com/ethereum/consensus-specs/blob/dev/specs/phase0/p2p-interface.md#the-gossip-domain-gossipsub
         * doge: https://github.com/dogecoin/dogecoin/blob/master/src/net_processing.cpp
         */
        public final int connectionTimeout;

        public BlockchainConfig(String name, String propagation, int blockSz, int txSz, int cbrBlockSz,
                int blockInterval, int timeout) {
            this.name = name;
            this.propagationProtocolName = propagation;
            this.blockSize = blockSz;
            this.transactionSize = txSz;
            this.CBRblockSize = cbrBlockSz;
            this.blockInterval = blockInterval;
            this.connectionTimeout = timeout;
        }
    }

    public static boolean RECORD_STATE_TRANSIT = false;

    /**
     * True if loading graph and precomputed centralities
     */
    public static String LOAD_GRAPH_PATH = "";

    public static Config SIM_CONFIGS;
    /**
     * if churn mode is on
     */
    public static Boolean CHURN_ON = false;

    /** 
     * For stability, a node (in GridRoutingTable) will send a message to 8
     * random nodes in addition to its row and column neighbors.
     */
    public static int RANDOM_CONNECTION = 8;
    
    /**
     * If true, the corrupted node behaves as crashed node: non-responding to any request, other non-responding only to full message request.
     */
    public static Boolean CORRUPTED_NODE_CRASH;


    /**
     * The kind of routing table.
     */
    public static String ROUTING_TABLE = "simblock.node.routing.RandomGraphRoutingTable";

    /**
     * The consensus algorithm to be used.
     */
    public static String CONSENSUS_CLASS = "simblock.node.consensus.ProofOfWork";


    /**
     * centrality class name
     */
    public static String CENTRALITY_CLASS = null;

    /**
     * The propagation mechanism used.
     */
    public static String PROPAGATION_CLASS = "";

    /**
     * The expected value of block generation interval. The difficulty of mining is
     * automatically
     * adjusted by this value and the sum of mining power. (unit: millisecond)
     */
    public static int BLOCK_INTERVAL = 1000 * 60 * 10;

    /**
     * The average mining power of each node. Mining power corresponds to Hash Rate
     * in Bitcoin, and
     * is the number of mining (hash calculation) executed per millisecond.
     */
    public static final int AVERAGE_MINING_POWER = 40;

    /**
     * The mining power of each node is determined randomly according to the normal
     * distribution
     * whose average is AVERAGE_MINING_POWER and standard deviation is
     * STDEV_OF_MINING_POWER.
     */
    public static final int STDEV_OF_MINING_POWER = 10;

    /**
     * The constant AVERAGE_COINS.
     */
    public static final int AVERAGE_COINS = 4000;
    /**
     * The constant STDEV_OF_COINS.
     */
    public static final int STDEV_OF_COINS = 2000;

    /**
     * The reward a PoS minter gets for staking.
     */
    public static final double STAKING_REWARD = 0.01;

    /**
     * The block height when a simulation ends.
     */
    protected static int END_BLOCK_HEIGHT = 100;

    /**
     * Block size. (unit: byte).
     */
    public static int BLOCK_SIZE = 80000;
    /*
     * default Transaction size (unit: byte)
     */
    public static int TRANSACTION_SIZE = 60000;

    /**
     * Shred size for CHUNK, derived from IPv6 min link MTU. (unit: byte).
     */
    public static final int SHRED_SIZE = 1280;

    /**
     * The usage rate of compact block relay (CBR) protocol.
     */
    public static final float CBR_USAGE_RATE = 0.964f;

    /**
     * Compact block size. (unit: byte)
     */
    public static int COMPACT_BLOCK_SIZE = 18 * 1000; // 18KB

    /**
     * CBR failure rate for a node that always connect network.
     */
    public static final float CBR_FAILURE_RATE_FOR_CONTROL_NODE = 0.13f;

    /**
     * CBR failure rate for a node that causes churn.
     */
    public static final float CBR_FAILURE_RATE_FOR_CHURN_NODE = 0.27f;

    /**
     * The distribution of data size that a control node receives when fails CBR.
     */
    public static final float[] CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE = {
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f
    };

    /**
     * The distribution of data size that a churn node receives when fails CBR.
     */
    public static final float[] CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE = {
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.03f, 0.03f, 0.03f,
            0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f,
            0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f,
            0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.03f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f,
            0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f, 0.04f,
            0.04f, 0.04f, 0.04f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f,
            0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f,
            0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.07f, 0.07f, 0.07f, 0.07f,
            0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.08f, 0.08f, 0.08f, 0.08f, 0.08f,
            0.08f, 0.08f, 0.08f, 0.08f, 0.08f, 0.08f, 0.08f, 0.09f, 0.09f, 0.09f, 0.09f, 0.09f, 0.09f, 0.09f, 0.09f,
            0.09f, 0.09f, 0.09f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.11f, 0.11f, 0.11f, 0.11f,
            0.11f, 0.11f, 0.11f, 0.11f, 0.11f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.13f, 0.13f,
            0.13f, 0.13f, 0.13f, 0.13f, 0.13f, 0.14f, 0.14f, 0.14f, 0.14f, 0.14f, 0.14f, 0.14f, 0.15f, 0.15f, 0.15f,
            0.15f, 0.15f, 0.15f, 0.16f, 0.16f, 0.16f, 0.16f, 0.16f, 0.16f, 0.17f, 0.17f, 0.17f, 0.17f, 0.17f, 0.18f,
            0.18f, 0.18f, 0.18f, 0.18f, 0.19f, 0.19f, 0.19f, 0.19f, 0.19f, 0.2f, 0.2f, 0.2f, 0.2f, 0.21f, 0.21f, 0.21f,
            0.21f, 0.22f, 0.22f, 0.22f, 0.22f, 0.23f, 0.23f, 0.23f, 0.23f, 0.24f, 0.24f, 0.24f, 0.24f, 0.25f, 0.25f,
            0.25f, 0.26f, 0.26f, 0.26f, 0.27f, 0.27f, 0.27f, 0.28f, 0.28f, 0.28f, 0.29f, 0.29f, 0.29f, 0.3f, 0.3f, 0.3f,
            0.31f, 0.31f, 0.31f, 0.32f, 0.32f, 0.32f, 0.33f, 0.33f, 0.34f, 0.34f, 0.35f, 0.35f, 0.36f, 0.36f, 0.37f,
            0.37f, 0.38f, 0.38f, 0.39f, 0.39f, 0.4f, 0.4f, 0.41f, 0.41f, 0.42f, 0.42f, 0.43f, 0.43f, 0.44f, 0.44f,
            0.45f,
            0.45f, 0.46f, 0.46f, 0.47f, 0.47f, 0.48f, 0.48f, 0.49f, 0.5f, 0.51f, 0.52f, 0.53f, 0.54f, 0.55f, 0.56f,
            0.57f, 0.58f, 0.59f, 0.6f, 0.61f, 0.62f, 0.63f, 0.64f, 0.65f, 0.66f, 0.67f, 0.68f, 0.69f, 0.7f, 0.71f,
            0.72f, 0.73f, 0.74f, 0.75f, 0.76f, 0.77f, 0.78f, 0.79f, 0.8f, 0.81f, 0.82f, 0.83f, 0.84f, 0.85f, 0.86f,
            0.87f, 0.88f, 0.89f, 0.9f, 0.91f, 0.92f, 0.93f, 0.94f, 0.95f, 0.96f
    };

    /*
     * Approximation window in ms, if the difference of the propagation latency
     * on two nodes is within the approximation window, count them as the same
     * and only record the first value. When latency is larger than
     * `APPROX_CUTOFF` then the window size is set to APPROX_WIN2 with bigger step.
     */
   public static final int APPROX_WIN1 = 5;
    // public static final int APPROX_WIN1 = 50;

    public static final int APPROX_WIN2 = 60000;

    public static final int APPROX_CUTOFF = 12000000;

    /*
     * percentage of nodes that request compact blocks
     */
    public static final float MONERO_FLUFFY_BLOCK_USAGE = 0.8f;

    /**
     * Real-world Solana validator stake distribution, i.e. the
     * `stake_proportion` column of
     * eval/solana/stake_distribution/data/stake_distribution_latest.csv
     * (fetched via fetch_stake_distribution.py), sorted descending by stake
     * and stripped of all other columns. Sums to 1.0 over 694 validators.
     * Used by {@link simblock.node.Node} to derive each simulated node's
     * stakeProportion regardless of the configured network size.
     */
    public static final float[] STAKE_PROPORTION_DISTRIBUTION = {
            3.937012542E-2f, 3.703488149E-2f, 2.808725466E-2f, 2.717977009E-2f, 2.117409839E-2f, 2.057180701E-2f,
            1.938395495E-2f, 1.837282928E-2f, 1.697363211E-2f, 1.510372381E-2f, 1.412416176E-2f, 1.384991966E-2f,
            1.374456048E-2f, 1.331874280E-2f, 1.075770138E-2f, 1.057722703E-2f, 9.413667598E-3f, 9.372070599E-3f,
            9.304383618E-3f, 9.195114699E-3f, 9.112540213E-3f, 9.026163271E-3f, 8.990695566E-3f, 8.595256706E-3f,
            8.336458738E-3f, 8.300179465E-3f, 8.129880933E-3f, 7.637046712E-3f, 7.232842413E-3f, 6.887566037E-3f,
            6.728411382E-3f, 6.671021273E-3f, 6.641093493E-3f, 6.002694081E-3f, 5.821944560E-3f, 5.772780523E-3f,
            5.772780493E-3f, 5.772734067E-3f, 5.772723886E-3f, 5.772706165E-3f, 5.767209987E-3f, 5.767209963E-3f,
            5.767207783E-3f, 5.767207662E-3f, 5.767207662E-3f, 5.648049648E-3f, 5.558245583E-3f, 5.358033649E-3f,
            5.306078463E-3f, 5.132428766E-3f, 5.035155172E-3f, 4.819860647E-3f, 4.806008685E-3f, 4.645603528E-3f,
            4.626426991E-3f, 4.469378605E-3f, 4.393294272E-3f, 4.386185756E-3f, 4.379548777E-3f, 4.340643543E-3f,
            4.325408050E-3f, 4.237744715E-3f, 4.234664143E-3f, 3.983619928E-3f, 3.870816574E-3f, 3.839385408E-3f,
            3.783306972E-3f, 3.740100064E-3f, 3.724484488E-3f, 3.615140700E-3f, 3.370422336E-3f, 3.356913913E-3f,
            3.351776377E-3f, 3.340010367E-3f, 3.236832892E-3f, 3.194164152E-3f, 3.175394355E-3f, 3.175385136E-3f,
            3.131453005E-3f, 3.091671702E-3f, 3.055768897E-3f, 3.055768897E-3f, 2.975879228E-3f, 2.887294675E-3f,
            2.873734465E-3f, 2.868228732E-3f, 2.860426564E-3f, 2.786667114E-3f, 2.693646268E-3f, 2.624537276E-3f,
            2.612694083E-3f, 2.521565503E-3f, 2.504010694E-3f, 2.461153153E-3f, 2.411404778E-3f, 2.372816029E-3f,
            2.349196741E-3f, 2.338386503E-3f, 2.308910124E-3f, 2.306883117E-3f, 2.119683398E-3f, 2.075733063E-3f,
            2.067696164E-3f, 2.066654066E-3f, 2.026063132E-3f, 1.980689008E-3f, 1.892724853E-3f, 1.866244221E-3f,
            1.846569141E-3f, 1.845214144E-3f, 1.841885902E-3f, 1.835984253E-3f, 1.834789257E-3f, 1.816351164E-3f,
            1.775373210E-3f, 1.774659859E-3f, 1.728838644E-3f, 1.728332994E-3f, 1.713020843E-3f, 1.666120945E-3f,
            1.660081295E-3f, 1.649509247E-3f, 1.605100422E-3f, 1.573211402E-3f, 1.505477367E-3f, 1.478753402E-3f,
            1.472263791E-3f, 1.464133978E-3f, 1.456637161E-3f, 1.449423834E-3f, 1.430695024E-3f, 1.427486913E-3f,
            1.418873163E-3f, 1.417448505E-3f, 1.414725070E-3f, 1.389058529E-3f, 1.337536173E-3f, 1.319006224E-3f,
            1.294314590E-3f, 1.277648210E-3f, 1.258931226E-3f, 1.258354704E-3f, 1.242141310E-3f, 1.239950063E-3f,
            1.235393936E-3f, 1.229479209E-3f, 1.223164576E-3f, 1.209194795E-3f, 1.197676280E-3f, 1.196137274E-3f,
            1.185861260E-3f, 1.183410506E-3f, 1.175322558E-3f, 1.142700359E-3f, 1.140598372E-3f, 1.130821152E-3f,
            1.126449555E-3f, 1.092951742E-3f, 1.085570453E-3f, 1.055888639E-3f, 1.010351527E-3f, 9.851623534E-4f,
            9.681726606E-4f, 9.605439766E-4f, 9.596806267E-4f, 9.409681587E-4f, 9.375929118E-4f, 9.374753432E-4f,
            9.273238242E-4f, 9.227626117E-4f, 9.198377859E-4f, 8.951612153E-4f, 8.920553539E-4f, 8.665708345E-4f,
            8.625252971E-4f, 8.588030829E-4f, 8.561633295E-4f, 8.288969672E-4f, 8.271472349E-4f, 8.240218629E-4f,
            8.215347157E-4f, 8.182477403E-4f, 8.066055324E-4f, 8.052683200E-4f, 8.039149642E-4f, 7.941922048E-4f,
            7.927071097E-4f, 7.831611982E-4f, 7.793509228E-4f, 7.782805745E-4f, 7.695646378E-4f, 7.516949978E-4f,
            7.357187526E-4f, 7.327675271E-4f, 7.278483402E-4f, 7.267228982E-4f, 7.258522909E-4f, 7.244483333E-4f,
            7.138809144E-4f, 7.128944950E-4f, 7.085669839E-4f, 7.058335146E-4f, 7.053656835E-4f, 6.944962965E-4f,
            6.936890489E-4f, 6.929523366E-4f, 6.920672377E-4f, 6.920603141E-4f, 6.867807052E-4f, 6.833375411E-4f,
            6.803496087E-4f, 6.799782887E-4f, 6.741483052E-4f, 6.735902074E-4f, 6.709076335E-4f, 6.696152500E-4f,
            6.638280186E-4f, 6.498204351E-4f, 6.476474913E-4f, 6.472737906E-4f, 6.461197018E-4f, 6.441023467E-4f,
            6.432204876E-4f, 6.425457028E-4f, 6.386787793E-4f, 6.359416949E-4f, 6.358970143E-4f, 6.354215626E-4f,
            6.273440270E-4f, 6.256932101E-4f, 6.246040625E-4f, 6.209415984E-4f, 6.207809955E-4f, 6.200618001E-4f,
            6.132306664E-4f, 6.114648627E-4f, 6.107023948E-4f, 6.099907451E-4f, 6.019306615E-4f, 6.002013969E-4f,
            6.001418383E-4f, 5.993961164E-4f, 5.958600919E-4f, 5.889660310E-4f, 5.885258092E-4f, 5.838172625E-4f,
            5.829883882E-4f, 5.818523633E-4f, 5.783315933E-4f, 5.768611035E-4f, 5.765262986E-4f, 5.749400119E-4f,
            5.732567711E-4f, 5.721009192E-4f, 5.713212190E-4f, 5.706837283E-4f, 5.694411082E-4f, 5.646548599E-4f,
            5.589659548E-4f, 5.582926016E-4f, 5.565724304E-4f, 5.515540224E-4f, 5.500347803E-4f, 5.358524731E-4f,
            5.352601091E-4f, 5.274556756E-4f, 5.229493554E-4f, 5.206799899E-4f, 5.201039970E-4f, 5.193207578E-4f,
            5.137133224E-4f, 5.086353397E-4f, 5.072819669E-4f, 5.066348152E-4f, 5.059550902E-4f, 5.039861343E-4f,
            5.034868349E-4f, 5.019058874E-4f, 5.013752130E-4f, 4.992764709E-4f, 4.950340812E-4f, 4.947681204E-4f,
            4.909800620E-4f, 4.877880625E-4f, 4.842630447E-4f, 4.799132701E-4f, 4.791169373E-4f, 4.784039582E-4f,
            4.771657790E-4f, 4.752678548E-4f, 4.750708921E-4f, 4.733273298E-4f, 4.731037851E-4f, 4.718232632E-4f,
            4.661523055E-4f, 4.656360694E-4f, 4.615146874E-4f, 4.598045286E-4f, 4.590255526E-4f, 4.582637314E-4f,
            4.570773666E-4f, 4.567919792E-4f, 4.557245429E-4f, 4.529852045E-4f, 4.512854431E-4f, 4.500660846E-4f,
            4.493492121E-4f, 4.480618938E-4f, 4.446148116E-4f, 4.427008453E-4f, 4.401414791E-4f, 4.398861114E-4f,
            4.393373402E-4f, 4.377930010E-4f, 4.368871790E-4f, 4.357790351E-4f, 4.346757037E-4f, 4.326484134E-4f,
            4.325596023E-4f, 4.312268373E-4f, 4.310492860E-4f, 4.310419207E-4f, 4.306367261E-4f, 4.300937423E-4f,
            4.296059421E-4f, 4.286966552E-4f, 4.276785617E-4f, 4.274215021E-4f, 4.262217093E-4f, 4.247259091E-4f,
            4.227515088E-4f, 4.225060179E-4f, 4.222199788E-4f, 4.195856267E-4f, 4.175124130E-4f, 4.174795053E-4f,
            4.169815383E-4f, 4.164519575E-4f, 4.163350214E-4f, 4.146180370E-4f, 4.097376178E-4f, 4.085487487E-4f,
            4.085454627E-4f, 4.084812637E-4f, 4.081521552E-4f, 4.064958454E-4f, 4.058902677E-4f, 4.056786549E-4f,
            4.045373139E-4f, 4.036193031E-4f, 3.971672024E-4f, 3.970924589E-4f, 3.968262410E-4f, 3.934771510E-4f,
            3.908154857E-4f, 3.895002314E-4f, 3.882956829E-4f, 3.868347318E-4f, 3.861245804E-4f, 3.854302343E-4f,
            3.844911098E-4f, 3.841946265E-4f, 3.837155747E-4f, 3.808603960E-4f, 3.804762018E-4f, 3.771900959E-4f,
            3.771657644E-4f, 3.757399277E-4f, 3.750488995E-4f, 3.748160243E-4f, 3.746951883E-4f, 3.680125033E-4f,
            3.658025879E-4f, 3.625700710E-4f, 3.625248563E-4f, 3.594155285E-4f, 3.580830179E-4f, 3.572610342E-4f,
            3.571271769E-4f, 3.568421392E-4f, 3.561649968E-4f, 3.502579649E-4f, 3.494435304E-4f, 3.493867154E-4f,
            3.489635680E-4f, 3.487863377E-4f, 3.478895159E-4f, 3.462188308E-4f, 3.446282023E-4f, 3.438740870E-4f,
            3.414460613E-4f, 3.369129173E-4f, 3.367065262E-4f, 3.351465216E-4f, 3.318715345E-4f, 3.312205203E-4f,
            3.310629172E-4f, 3.303577445E-4f, 3.297517860E-4f, 3.285957305E-4f, 3.284997384E-4f, 3.279086612E-4f,
            3.264914299E-4f, 3.263853190E-4f, 3.259345378E-4f, 3.214999150E-4f, 3.212191991E-4f, 3.209109876E-4f,
            3.207942560E-4f, 3.195842252E-4f, 3.139999425E-4f, 3.125357109E-4f, 3.124911817E-4f, 3.117150786E-4f,
            3.114065115E-4f, 3.088965606E-4f, 3.082097552E-4f, 3.067997256E-4f, 3.048853542E-4f, 3.036222528E-4f,
            3.032075339E-4f, 3.025419641E-4f, 3.014814134E-4f, 2.994118252E-4f, 2.974618981E-4f, 2.966104526E-4f,
            2.962205159E-4f, 2.957431086E-4f, 2.950895270E-4f, 2.932716820E-4f, 2.932604058E-4f, 2.916496216E-4f,
            2.904737375E-4f, 2.904045331E-4f, 2.902039598E-4f, 2.892621175E-4f, 2.863161486E-4f, 2.845952282E-4f,
            2.820399469E-4f, 2.815010989E-4f, 2.794966672E-4f, 2.792738943E-4f, 2.774444036E-4f, 2.772901843E-4f,
            2.709491754E-4f, 2.695138678E-4f, 2.692262529E-4f, 2.688008096E-4f, 2.682650817E-4f, 2.656861148E-4f,
            2.655580675E-4f, 2.655540897E-4f, 2.648668181E-4f, 2.638481464E-4f, 2.634729271E-4f, 2.624778192E-4f,
            2.623578480E-4f, 2.623467026E-4f, 2.587971745E-4f, 2.587499154E-4f, 2.585106957E-4f, 2.584937128E-4f,
            2.577050083E-4f, 2.575353432E-4f, 2.572711308E-4f, 2.569387148E-4f, 2.545572245E-4f, 2.541207952E-4f,
            2.537074981E-4f, 2.536190750E-4f, 2.534115826E-4f, 2.533751421E-4f, 2.523452407E-4f, 2.522725587E-4f,
            2.515347035E-4f, 2.507584353E-4f, 2.496163273E-4f, 2.489058177E-4f, 2.487231314E-4f, 2.486306021E-4f,
            2.479465532E-4f, 2.478412059E-4f, 2.476247148E-4f, 2.474064958E-4f, 2.473211091E-4f, 2.453560370E-4f,
            2.451923785E-4f, 2.445940414E-4f, 2.433333015E-4f, 2.427617415E-4f, 2.419345882E-4f, 2.414796973E-4f,
            2.413809053E-4f, 2.411377792E-4f, 2.374213905E-4f, 2.360522982E-4f, 2.350903109E-4f, 2.349919252E-4f,
            2.339048488E-4f, 2.329295969E-4f, 2.326745149E-4f, 2.324046815E-4f, 2.323131265E-4f, 2.322085039E-4f,
            2.319060236E-4f, 2.317861884E-4f, 2.315789071E-4f, 2.313291474E-4f, 2.311774472E-4f, 2.304799492E-4f,
            2.300107492E-4f, 2.294386084E-4f, 2.270503629E-4f, 2.264416754E-4f, 2.251442576E-4f, 2.251247844E-4f,
            2.249559821E-4f, 2.245681268E-4f, 2.241334277E-4f, 2.240993727E-4f, 2.222990404E-4f, 2.216150657E-4f,
            2.211637439E-4f, 2.211061064E-4f, 2.207520908E-4f, 2.187204311E-4f, 2.159099157E-4f, 2.137113845E-4f,
            2.136907565E-4f, 2.122451473E-4f, 2.120947636E-4f, 2.105140525E-4f, 2.103897459E-4f, 2.100606314E-4f,
            2.083951424E-4f, 2.052755147E-4f, 2.035031737E-4f, 2.020925032E-4f, 2.010802184E-4f, 2.005539792E-4f,
            1.978635248E-4f, 1.962314786E-4f, 1.956938008E-4f, 1.944688473E-4f, 1.934647116E-4f, 1.929201729E-4f,
            1.929118985E-4f, 1.927802091E-4f, 1.925219929E-4f, 1.922666194E-4f, 1.919339992E-4f, 1.908982518E-4f,
            1.908692253E-4f, 1.907832820E-4f, 1.896973737E-4f, 1.889145092E-4f, 1.885187832E-4f, 1.870615715E-4f,
            1.870318521E-4f, 1.860224152E-4f, 1.859286609E-4f, 1.857171689E-4f, 1.851009322E-4f, 1.845549894E-4f,
            1.840702387E-4f, 1.838117249E-4f, 1.836642804E-4f, 1.829991560E-4f, 1.812503866E-4f, 1.811977059E-4f,
            1.805431437E-4f, 1.772739833E-4f, 1.762464625E-4f, 1.744941540E-4f, 1.730162928E-4f, 1.707110510E-4f,
            1.692239727E-4f, 1.683793009E-4f, 1.606035886E-4f, 1.601479230E-4f, 1.582003510E-4f, 1.575801846E-4f,
            1.563110977E-4f, 1.561727702E-4f, 1.535624862E-4f, 1.528803063E-4f, 1.507794676E-4f, 1.495209347E-4f,
            1.480196572E-4f, 1.473440732E-4f, 1.459320950E-4f, 1.444198013E-4f, 1.424265385E-4f, 1.405013162E-4f,
            1.400894454E-4f, 1.398618518E-4f, 1.396172997E-4f, 1.387830736E-4f, 1.374463544E-4f, 1.351157465E-4f,
            1.350532176E-4f, 1.350061129E-4f, 1.348435353E-4f, 1.348271366E-4f, 1.347439389E-4f, 1.345425974E-4f,
            1.344948542E-4f, 1.342243322E-4f, 1.334485248E-4f, 1.325319410E-4f, 1.323022681E-4f, 1.320998819E-4f,
            1.315037951E-4f, 1.300764742E-4f, 1.299091221E-4f, 1.266552160E-4f, 1.244673286E-4f, 1.244413433E-4f,
            1.239763497E-4f, 1.235144454E-4f, 1.188976385E-4f, 1.165842250E-4f, 1.150223059E-4f, 1.148113108E-4f,
            1.136896274E-4f, 1.102054018E-4f, 1.014769940E-4f, 1.012273485E-4f, 1.009927589E-4f, 1.006328652E-4f,
            9.704528757E-5f, 9.430411903E-5f, 9.124816732E-5f, 8.999234624E-5f, 8.984113013E-5f, 8.601338529E-5f,
            8.476186063E-5f, 8.350556219E-5f, 8.268406428E-5f, 8.091968800E-5f, 7.865710323E-5f, 7.716169629E-5f,
            7.572746823E-5f, 7.538508159E-5f, 7.311240182E-5f, 6.859065491E-5f, 6.181861657E-5f, 6.151669337E-5f,
            6.091517001E-5f, 5.849431989E-5f, 5.752734050E-5f, 5.714482575E-5f, 5.537058767E-5f, 5.436038716E-5f,
            5.202585856E-5f, 4.805664042E-5f, 4.677951939E-5f, 4.511239000E-5f, 4.193083297E-5f, 4.016635040E-5f,
            3.686678534E-5f, 3.644442138E-5f, 3.517044719E-5f, 3.507247786E-5f, 3.480830502E-5f, 3.432332616E-5f,
            3.304868107E-5f, 3.041776047E-5f, 2.951600994E-5f, 2.669017701E-5f, 2.549311333E-5f, 2.487576353E-5f,
            2.476853608E-5f, 2.350111155E-5f, 2.331305191E-5f, 1.791323250E-5f, 1.585245677E-5f, 1.557249342E-5f,
            1.387177261E-5f, 6.908796632E-6f, 5.479964788E-6f, 5.309729466E-6f, 5.047829912E-6f, 4.612655878E-6f,
            3.124978998E-6f, 2.893738865E-6f, 1.708048934E-6f, 1.470095228E-6f, 1.202952047E-6f, 3.214638030E-7f,
            2.306883120E-9f, 1.150482106E-9f, 4.665325078E-12f, 0.000000000E0f
    };

    /**
     * flag to signal the usage of transactions in the simulation
     */
    public static boolean USE_TRANSACTIONS = false;

    /**
     * enables additional log file
     * (not thread save)
     */
    public static boolean DEBUG_MODE = false;

    /**
     * number of transactions created with the genesis block
     */
    public static int INITIAL_TRANSACTIONS = 5;

    /**
     * number of high bandwidth connection in CBR propagation
     */
    public static int CBR_HIGH_BW_CONNECTIONS = 3;

    /**
     * reduce the number of concurrent mining task for the same blog in the task
     * queue
     */
    public static boolean FILTER_MINING_TASKS = false;
    public static int MAX_MINING_TASKS = 100;

    public static final int getEndBlockHeight() {
        return END_BLOCK_HEIGHT;
    }

    public static final BlockchainConfig[] blockchainConfigs = {
            /* Bitcoin */
            new BlockchainConfig("btc", "Cbr", 1700000, 60000, 17807, 600000, 600000),
            /* Ethereum */
            new BlockchainConfig("eth", "Hybrid", 1200, 40, 1200, 13000, 5000),
            /* Doge coin */
            new BlockchainConfig("doge", "Cbr", 15000, 30, 15000, 60000, 60000),
            /* Monero */
            new BlockchainConfig("monero", "Monero", 79900, 390, 1266, 120000, 0),
            /* Cardano */
            new BlockchainConfig("cardano", "Cardano", 31000, 101, 31000, 20000, 20000),

            new BlockchainConfig("rumor", "Push", 500, 500, 500, 0, 0)
    };
}
