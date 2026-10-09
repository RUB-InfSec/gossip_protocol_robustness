package simblock.settings;

import simblock.node.consensus.RumorSpreading;
import simblock.node.propagation.Chunk;
import simblock.node.propagation.Push;
import simblock.node.routing.GossipsubRoutingTable;
import simblock.node.routing.SampleRoutingTable;
import simblock.node.routing.UndirectedRoutingTable;
import simblock.settings.SimulationConfiguration.BlockchainConfig;
import simblock.simulator.Main;
import simblock.simulator.Simulator;

public class Config {
    public RunOpt run;
    public NetworkOpt network;
    public BlockchainOpt blockchain;
    public GossipOpt gossip;
    public AdversaryOpt adversary;

    public static class RunOpt {
        public String version;
        public int number;
        public int numBlocks;
    }

    public static class NetworkOpt {
        public NetworkTopology topology;
        public String sampleTopology;
        public int size; // num of nodes
        public int routingTableSize;    // NOTE not used
        public int degree;  // used to generate G(n,p) ER graph
        public int edges; // used to generate G(n,m) ER graph
        public GossipSubOpt gossipsub;
        public float churnRate;
        public float pPrivateNodes;
        /**
         * Overrides {@link NetworkConfiguration#SIMULATE_REAL_NETWORK_LATENCY} when set.
         * Left {@code null} (unset in yaml) to keep the built-in default.
         */
        public Boolean realLatency;
    }

    public static class BlockchainOpt {
        public BlockchainNetwork type;
        public MiningDistribution miningDist;
        public boolean useTransaction;
        public boolean fixedProfile;
        public boolean filterMiningTask;
    }

    public static class GossipOpt {
        public TransmissionModel model;
        public int messageSize;
        public double arrivalRatePerSec;
        public int timeoutS;
        public EcConfig erasureCode;
        public int randomConnection;
    }

    public static class GossipSubOpt {
        public int meshTarget;
        public int meshLo;
        public int meshHi;
        public int gossipHeartbeatIntervalMS;
        public int numGossipPeers;
    }

    public static class AdversaryOpt {
        public AdversaryStrategyType strategy;
        public boolean globalKnowledge;
        public boolean adaptive;
        public NodeCentralityType nodeCentrality;
        public double pFailedNodes;
        public double pFailedLinks;
        public double corruptionDelayS;
        public CorruptBehaviorType corruptBehavior;
        public double msgDropRate;
    }

    /** Erasure coding configuration for CHUNK */
    public static class EcConfig {
        public int k;
        public int m;
    }

    public enum NetworkTopology {
        /** random graph */
        RANDOM_GRAPH,
        /** geographically clustered graph */
        GOSSIPSUB,
        /** grid topology as a 2D matrix */
        GRID,
        /** bitcoin core **/
        BITCOIN,
        /** ethereum classic for testnet Ropsten  **/
        ETHEREUM_CLASSIC,
        /** imported from a given topology */
        SAMPLE
    }

    public enum MiningDistribution {
        /** uniformly random distribution */
        DEFAULT,
        /** uniformly random dist. + 10k zero nodes */
        DEFAULT_Z,
        /** uniformly random dist. 500,000 / 5000 zero nodes */
        ZERONODES,
        /** Pool count A or B (Pool count B is used in solana) */
        MININGPOOLS;
    }

    public enum BlockchainNetwork {
        BTC, ETH, DOGE, MONERO, CARDANO, RUMOR;
    }

    public enum TransmissionModel {
        PUSH, ADVERTISEMENT, HYBRID, GOSSIPSUB, CBR, CHUNK;
    }

    public enum AdversaryStrategyType {
        NONE, RANDOM, GREEDY, WEIGHTED
    }

    public enum NodeCentralityType {
        DEGREE, CLOSENESS, BETWEENNESS, EIGEN
    }

    public enum CorruptBehaviorType {
        CRASH, NONRESPONDING
    }

    public boolean validateOpts() {
        if (network.churnRate >= 1 || network.churnRate < 0) {
            System.err.println("network.churnRate must be between 0 and 1.");
            return false;
        }
        if (network.pPrivateNodes >= 1 || network.pPrivateNodes < 0) {
            System.err.println("network.pPrivateNodes must be between 0 and 1.");
            return false;
        }
        if (adversary.pFailedNodes >= 1 || adversary.pFailedNodes < 0) {
            System.err.println("adversary.failedNodes must be between 0 and 1.");
            return false;
        }
        if (adversary.pFailedLinks >= 1 || adversary.pFailedLinks < 0) {
            System.err.println("adversary.failedLinks must be between 0 and 1.");
            return false;
        }
        return true;
    }

    public void applySimulationSettings() {
        if (run.version.contains("debug")) {
            SimulationConfiguration.DEBUG_MODE = true;
        }
        SimulationConfiguration.END_BLOCK_HEIGHT = run.numBlocks;
        if (network.routingTableSize == 0) {
            network.routingTableSize = network.size;
        }
        NetworkConfiguration.ROUTING_TABLE_SIZE = network.routingTableSize;
        NetworkConfiguration.DEGREE = network.degree;
        NetworkConfiguration.CHURN_RATE = network.churnRate;
        if (network.realLatency != null) {
            NetworkConfiguration.SIMULATE_REAL_NETWORK_LATENCY = network.realLatency;
        }
        if (network.churnRate > 0) {
            SimulationConfiguration.CHURN_ON = true;
        } else {
            SimulationConfiguration.CHURN_ON = false;
        }

        // overwrite the network size according to the sample network
        if (network.topology == NetworkTopology.SAMPLE) {
            SampleRoutingTable.loadTopology(Main.class.getResourceAsStream("/" + network.sampleTopology));
            network.size = SampleRoutingTable.getNetworkSize();
        }

        // Now set config if applied
        if (blockchain.type == BlockchainNetwork.RUMOR) {
            if (gossip.model == TransmissionModel.CHUNK) {
                SimulationConfiguration.ROUTING_TABLE = UndirectedRoutingTable.class.getName();
            } else {
                SimulationConfiguration.ROUTING_TABLE = "simblock.node.routing." + convertToClassPrefix(network.topology.toString()) + "RoutingTable";
            }
            RumorSpreading.initArrivalRate(gossip.arrivalRatePerSec);
            // only record 100 times the evolution of peer score
            Simulator.setScoreRecordPeriod((int) Math.ceil(run.numBlocks/gossip.arrivalRatePerSec*1000/100));
            blockchain.miningDist = MiningDistribution.DEFAULT;
            SimulationConfiguration.CONSENSUS_CLASS = "simblock.node.consensus.RumorSpreading";
            SimulationConfiguration.PROPAGATION_CLASS = "simblock.node.propagation." + convertToClassPrefix(gossip.model.toString());
            SimulationConfiguration.CENTRALITY_CLASS = "simblock.graph.centrality." + convertToClassPrefix(adversary.nodeCentrality.toString()) + "Centrality";
            SimulationConfiguration.BLOCK_SIZE = gossip.messageSize;
            SimulationConfiguration.USE_TRANSACTIONS = false;
            SimulationConfiguration.FILTER_MINING_TASKS = false;
            SimulationConfiguration.RANDOM_CONNECTION = gossip.randomConnection;
            NetworkConfiguration.TIMTOUT = gossip.timeoutS;
            if (adversary.corruptBehavior == CorruptBehaviorType.CRASH) {
                SimulationConfiguration.CORRUPTED_NODE_CRASH = true;
            } else if (adversary.corruptBehavior == CorruptBehaviorType.NONRESPONDING) {
                SimulationConfiguration.CORRUPTED_NODE_CRASH = false;
            }
            // Solana
            if (gossip.model == TransmissionModel.CHUNK) {
                Chunk.setErasureCodeConfig(gossip.erasureCode.k, gossip.erasureCode.m);
                SimulationConfiguration.PROPAGATION_CLASS = "simblock.node.propagation." + convertToClassPrefix(gossip.model.toString());
                SimulationConfiguration.CORRUPTED_NODE_CRASH = true;
            }
            if (gossip.model == TransmissionModel.GOSSIPSUB) {
                SimulationConfiguration.ROUTING_TABLE = GossipsubRoutingTable.class.getName();
            }
            // Polkadot
            if (network.topology == NetworkTopology.GRID) {
                SimulationConfiguration.PROPAGATION_CLASS = Push.class.getName();
                SimulationConfiguration.CORRUPTED_NODE_CRASH = true;
            }
        } else {
            applyBlockchainSettings();
            SimulationConfiguration.ROUTING_TABLE = "simblock.node.routing.RandomGraphRoutingTable";
            SimulationConfiguration.CONSENSUS_CLASS = "simblock.node.consensus.ProofOfWork";
            SimulationConfiguration.USE_TRANSACTIONS = blockchain.useTransaction;
            SimulationConfiguration.FILTER_MINING_TASKS = blockchain.filterMiningTask;
        }
    }

    private void applyBlockchainSettings() {
        for (BlockchainConfig cfg : SimulationConfiguration.blockchainConfigs) {
            if (cfg.name.equalsIgnoreCase(blockchain.type.toString())) {
                SimulationConfiguration.PROPAGATION_CLASS = "simblock.node.propagation." + cfg.propagationProtocolName;
                SimulationConfiguration.BLOCK_SIZE = cfg.blockSize;
                SimulationConfiguration.TRANSACTION_SIZE = cfg.transactionSize;
                SimulationConfiguration.COMPACT_BLOCK_SIZE = cfg.CBRblockSize;
                SimulationConfiguration.BLOCK_INTERVAL = cfg.blockInterval;
                NetworkConfiguration.TIMTOUT = cfg.connectionTimeout;

                if (blockchain.fixedProfile) {
                    SimulationConfiguration.BLOCK_SIZE = 1700000;
                    SimulationConfiguration.TRANSACTION_SIZE = 60000;
                    SimulationConfiguration.COMPACT_BLOCK_SIZE = 17807;
                    // BLOCK_INTERVAL = 600000;
                    // NetworkConfiguration.TIMTOUT = 600000;
                }
            }
        }
    }

    String convertToClassPrefix(String enumType) {
        String prefix = "";
        for (String seg : enumType.split("_")) {
            prefix += seg.substring(0, 1) + seg.substring(1).toLowerCase();
        }
        return prefix;
    }

    public String getConfStr(String miningDist) {
        String conf;
        if (blockchain.type == BlockchainNetwork.RUMOR) {
            conf = String.join("_",
                    run.version,
                    String.valueOf(run.number),
                    blockchain.type.toString(),
                    gossip.model.toString(),
                    "s" + String.valueOf(gossip.messageSize),
                    "r" + String.format("%.5f", gossip.arrivalRatePerSec));
        } else {
            conf = String.join("_",
                    run.version,
                    String.valueOf(run.number),
                    "dist-" + miningDist,
                    blockchain.type.toString(),
                    SimulationConfiguration.PROPAGATION_CLASS.split("\\.")[3],
                    blockchain.fixedProfile ? "fixedPf" : "diffPf",
                    blockchain.useTransaction ? "tx" : "noTx",
                    blockchain.filterMiningTask ? "filterBlk" : "noFilter");
        }
        return String.join("_", conf, adversary.strategy.toString(), adversary.adaptive?"ADAPT":"STATIC");
    }
}
