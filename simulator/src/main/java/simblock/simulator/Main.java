/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 u
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.simulator;

import simblock.node.routing.GossipsubRoutingTable;
import simblock.node.routing.RandomGraphRoutingTable;
import simblock.settings.SimulationConfiguration;
import simblock.task.AbstractMessageTask;
import simblock.task.SampleNetworkStatTask;
import simblock.task.Task;
import simblock.settings.Config;
import simblock.settings.Config.MiningDistribution;
import simblock.settings.NetworkConfiguration;
import simblock.adversary.Adversary;
import simblock.node.Node;
import simblock.node.propagation.Cbr;

import static simblock.simulator.SimulationLoader.loadNetworkConstruction;
import static simblock.simulator.Simulator.simRng;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameter;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * The type Main represents the entry point.
 */
public class Main {

    /**
     * The initial simulation time.
     */
    static long simulationTime = 0;

    static Config simulatorConfig;

    /**
     * Output path.
     */
    static URI OUT_FILE_URI;
    static PrintWriter OUT_STAT_FILE;

    /**
     * The constant STATIC_JSON_FILE.
     */
    public static PrintWriter SCORE_STAT_FILE;

    static {
        try {
            Path outPath = Paths.get("").toAbsolutePath().resolve("src/dist/output");
            if (Files.exists(outPath) && Files.isDirectory(outPath)) {
                OUT_FILE_URI = outPath.toUri();
            } else {
                OUT_FILE_URI = Paths.get("").toAbsolutePath().resolve("output").toUri();
                if (!Files.exists(Paths.get(OUT_FILE_URI))) {
                    Files.createDirectories(Paths.get(OUT_FILE_URI));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Arguments {
        @Parameter(names = { "-c", "--config" }, description = "Path to the .yaml config file")
        public String configFile = "";

        @Parameter(names = { "-o", "--output" }, description = "Path to the .json output file")
        public String outputFile = "";

        @Parameter(names = { "-l", "--load" }, description = "DIR path to the .json graph file that is used to load the ER graph and shortest paths. The file name should be inferred from configuration")
        public String loadGraphPath = "";

        @Parameter(names = { "--prepare-networksize" }, description = "Take the network sizes and for each network size, generate 10 random graphs and pre-calculate its shortest paths")
        public List<Integer> prepareNetworkSizes = new ArrayList<>();

        @Parameter(names = { "--prepare-networkdegree" }, description = "Take a list of degrees for each network degree, generate 10 random graphs and pre-calculate its shortest paths")
        public List<Integer> prepareDegrees = new ArrayList<>();
    }

    /**
     * The entry point.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Arguments arguments = new Arguments();
        JCommander commander = JCommander.newBuilder()
            .addObject(arguments)
            .build();
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            commander.usage(); // Show valid options
            return;
        }

        if (!arguments.prepareNetworkSizes.isEmpty()) {
            try {
                SimulationLoader.prepareERGraphs(arguments.prepareNetworkSizes, arguments.prepareDegrees);
            } catch (IOException e) {
                System.out.println("Failed to save ER graph: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            return;
        }

        SimulationConfiguration.LOAD_GRAPH_PATH = arguments.loadGraphPath;

        InputStream configInStream;
        try {
            if (arguments.configFile.isEmpty()) {
                System.out.println(
                        "No configuration file specified. Searching for config file 'config.yaml' in resources.");
                configInStream = Main.class.getResourceAsStream("/config.yaml");
            } else {
                Path cfgFile = Paths.get(arguments.configFile);
                configInStream = Files.newInputStream(cfgFile);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to open config file.");
            e.printStackTrace();
            return;
        }
        LoaderOptions loaderOpt = new LoaderOptions();
        loaderOpt.setEnumCaseSensitive(false);
        Yaml yaml = new Yaml(new Constructor(Config.class, loaderOpt));
        simulatorConfig = yaml.load(configInStream);
        if (!simulatorConfig.validateOpts()) {
            return;
        }

        simulatorConfig.applySimulationSettings();
        // Must be set before any Node is constructed: Node's constructor reads
        // SIM_CONFIGS.network.size to derive each node's stakeProportion.
        SimulationConfiguration.SIM_CONFIGS = simulatorConfig;

        simRng = new Random(simulatorConfig.run.number);
        System.out.println("Starting new run with run number: " + simulatorConfig.run.number);

        final long start = System.currentTimeMillis();

        String configname = "";
        String graphFilePath = "";
        if (!SimulationConfiguration.LOAD_GRAPH_PATH.isEmpty()) {
            graphFilePath = String.format("%s/graph_%d_%d.%d.json", SimulationConfiguration.LOAD_GRAPH_PATH,
                    simulatorConfig.network.size, simulatorConfig.network.degree, simulatorConfig.run.number);
            if (!(new File(graphFilePath)).exists()) {
                graphFilePath = "";
            }
        }
        if (!graphFilePath.isEmpty()) {
            Config.NodeCentralityType cenType = simulatorConfig.adversary.nodeCentrality;
            if (simulatorConfig.adversary.strategy != Config.AdversaryStrategyType.GREEDY) {
                cenType = Config.NodeCentralityType.DEGREE;
            }
            loadNetworkConstruction(graphFilePath, simulatorConfig.network.size, cenType);
            // connect nodes
            for (Node node : Simulator.getSimulatedNodes()) {
                node.joinNetwork(false);
            }
            GossipsubRoutingTable.graph = null;
        } else {
            String miningDist = constructNetworkByMiningDist(simulatorConfig.network.size, simulatorConfig.blockchain.miningDist);
            configname = simulatorConfig.getConfStr(miningDist);
        }
        try {
            if (arguments.outputFile.isEmpty()) {
                OUT_STAT_FILE = new PrintWriter(
                        new BufferedWriter(new FileWriter(
                                new File(OUT_FILE_URI
                                    .resolve(configname + "_n" + simulatorConfig.network.size + ".json")))));
                if (simulatorConfig.gossip.model == Config.TransmissionModel.GOSSIPSUB) {
                    SCORE_STAT_FILE = new PrintWriter(
                            new BufferedWriter(new FileWriter(
                                    new File(OUT_FILE_URI
                                            .resolve("score-" + configname + "_n" + simulatorConfig.network.size + ".json")))));
                }
            } else {
                OUT_STAT_FILE = new PrintWriter(new BufferedWriter(new FileWriter(arguments.outputFile)));
                if (simulatorConfig.gossip.model == Config.TransmissionModel.GOSSIPSUB) {
                    SCORE_STAT_FILE = new PrintWriter(new BufferedWriter(new FileWriter(arguments.outputFile.replace("output_", "scores_"))));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ------ Start Simulation -------
        // Adversary must be initialized after the network is constructed for the adversary to init the topology matrix.
        int numPublicNodes = (int) (simulatorConfig.network.size * (1-simulatorConfig.network.pPrivateNodes));
        Adversary adversary = new Adversary(simulatorConfig.adversary, numPublicNodes);
        if (!adversary.isAdaptive()) {
            adversary.staticSetup();
        }
        // The first node triggers the propagation of the genesis block
        startSimulation();
        // Iterate over tasks and handle, simulation will eventually
        // stop when no further mining task is scheduled.
        while (Timer.hasNextTask()) {
            Task task = Timer.runTask();
            if (adversary.isAdaptive() && !adversary.isAttackInProgress()) {
                if (task instanceof AbstractMessageTask) {
                    adversary.checkAttackOpportunity((AbstractMessageTask) task);
                }
            }
        }
        // Print propagation information about all blocks
        Simulator.printStats(OUT_STAT_FILE);
        if (simulatorConfig.gossip.model == Config.TransmissionModel.GOSSIPSUB && simulatorConfig.run.number == 11) {
            Simulator.printGossipScoreStats(SCORE_STAT_FILE);
        }

        long end = System.currentTimeMillis();
        simulationTime = end - start;
        System.out.println("Time spent : " + (int) (simulationTime / 1000) + "s N=" + simulatorConfig.network.size + ", conf: "
                + configname);
        System.out.printf("Simulation time: %,d\n", Timer.getCurrentTime());
        System.out.printf("Number of tasks: %,d\n", Timer.TASK_COUNTS);
        System.out.printf("Memory consumption: %,d MB\n", (java.lang.Runtime.getRuntime().totalMemory() - java.lang.Runtime.getRuntime().freeMemory())/(1024*1024));
    }

    public static String constructNetworkByMiningDist(int networkSize, MiningDistribution miningDistribution) {
        String miningDist = miningDistribution.toString().toLowerCase();
        // internal variables for experiment 2(zeronodes) and experiment 3(miningpools)
        int internalSmallMinerNumber = 0;
        int internalZeroNodesNumber = 0;

        // Setup network
        switch (miningDistribution) {
            case MiningDistribution.ZERONODES:
                NetworkConfiguration.setUSEMININGPOOLS(false);
                NetworkConfiguration.setPoolCount(0);
                internalZeroNodesNumber = 5000;
                // internalZeroNodesNumber = 500000;
                miningDist = "zeronodes-" + internalZeroNodesNumber;
                constructNetwork(networkSize, internalZeroNodesNumber, false);
                break;
            case MiningDistribution.DEFAULT_Z:
                NetworkConfiguration.setUSEMININGPOOLS(false);
                internalZeroNodesNumber = 10000;
                miningDist = "defaultz-" + internalZeroNodesNumber;
                constructNetwork(networkSize, internalZeroNodesNumber, false);
                break;
            case MiningDistribution.MININGPOOLS:
                NetworkConfiguration.setUSEMININGPOOLS(true);
                NetworkConfiguration.setPoolCount(NetworkConfiguration.PoolCount_B);
                NetworkConfiguration.setPoolProportions(NetworkConfiguration.PoolProportion_B);
                internalSmallMinerNumber = 0;
                // NetworkConfiguration.setPoolCount(NetworkConfiguration.PoolCount_A);
                // NetworkConfiguration.setPoolProportions(NetworkConfiguration.PoolProportion_A);
                // internalSmallMinerNumber = 10000;
                miningDist = "miningpools-" + (NetworkConfiguration.getPoolCount() + internalSmallMinerNumber);
                constructNetwork(NetworkConfiguration.getPoolCount() + internalSmallMinerNumber, networkSize, true);
                break;
            default:
                NetworkConfiguration.setUSEMININGPOOLS(false);
                constructNetwork(networkSize, 0, false);
                break;
        }
        return miningDist;
    }

    /**
     * Populate the list using the distribution.
     *
     * @param distribution the distribution
     * @param facum        whether the distribution is cumulative distribution
     * @return array list
     */
    public static ArrayList<Integer> makeRandomListFollowDistribution(double[] distribution, boolean facum,
            int size) {
        ArrayList<Integer> list = new ArrayList<>();
        int index = 0;
        if (facum) {
            for (; index < distribution.length; index++) {
                while (list.size() <= size * distribution[index]) {
                    list.add(index);
                }
            }
            while (list.size() < size) {
                list.add(index);
            }
        } else {
            double acumulative = 0.0;
            int lastValidIndex = 0;
            for (; index < distribution.length; index++) {
                if (distribution[index] <= 0) {
                    // Negative probability: exclude this region from sampling entirely.
                    continue;
                }
                acumulative += distribution[index];
                lastValidIndex = index;
                while (list.size() <= size * acumulative) {
                    list.add(index);
                }
            }
            while (list.size() < size) {
                list.add(lastValidIndex);
            }
        }
        Collections.shuffle(list, simRng);
        return list;
    }

    /**
     * Populate the list using the rate.
     *
     * @param rate the rate of true
     * @return array list
     */
    public static ArrayList<Boolean> makeRandomList(float rate, int size) {
        ArrayList<Boolean> list = new ArrayList<Boolean>();
        for (int i = 0; i < size; i++) {
            list.add(i < size * rate);
        }
        Collections.shuffle(list, simRng);
        return list;
    }

    /**
     * Generates a random mining power expressed as Hash Rate, and is the number of
     * mining (hash
     * calculation) executed per millisecond.
     *
     * @return the number of hash calculations executed per millisecond.
     */
    public static int genMiningPower() {
        double r = simRng.nextGaussian();
        return Math.max((int) (r * SimulationConfiguration.STDEV_OF_MINING_POWER
                    + SimulationConfiguration.AVERAGE_MINING_POWER), 1);
    }

    /**
     * @param numNodes
     * @return return the mining power every regular node receives
     */
    public static long calcMiningPower(int numNodes) {
        long totalMininingPower = NetworkConfiguration.getNetworkMiningPower();
        if (NetworkConfiguration.getUSEMINGPOOLS()) {
            Double allPools = 0.0;
            for (int i = 0; i < NetworkConfiguration.getPoolProportion().length; i++) {
                allPools += NetworkConfiguration.getPoolProportion()[i];
            }
            long poolPower = Math.round((totalMininingPower / 100) * allPools);
            if (numNodes != NetworkConfiguration.PoolCount) {
                return (totalMininingPower - poolPower) / (numNodes - NetworkConfiguration.PoolCount);
            } else {
                return 0;
            }
        } else {
            return totalMininingPower / (numNodes);
        }
    }

    public static void constructNetwork(int miners, int zeronodes, boolean connectedMiners) {
        double[] regionDistribution = Network.getRegionDistribution();
        List<Integer> regionList = makeRandomListFollowDistribution(regionDistribution, false, miners + zeronodes);
        // List of churn nodes.
        List<Boolean> churnNodes = makeRandomList(NetworkConfiguration.CHURN_RATE, miners + zeronodes);
        long miningPower;
        long avg_miningPower = calcMiningPower(miners);
        int networkSize = miners + zeronodes;

        if (simulatorConfig == null || simulatorConfig.gossip.model != Config.TransmissionModel.CHUNK) {
            switch (SimulationConfiguration.ROUTING_TABLE.toUpperCase()) {
                case String s when s.contains("RANDOM") -> {
                    if (NetworkConfiguration.DEGREE > 0) {
                        RandomGraphRoutingTable.generateRandomGraphNP(networkSize, NetworkConfiguration.DEGREE * 1.0 / networkSize);
                    } else {
                        RandomGraphRoutingTable.generateRandomGraphNM(networkSize, simulatorConfig.network.edges);
                    }
                }
                case String s when s.contains("GOSSIPSUB") -> {
                    GossipsubRoutingTable.setConfig(NetworkConfiguration.DEGREE, simulatorConfig.network.gossipsub);
                    GossipsubRoutingTable.generateMeshGraph(networkSize);
                }
                default -> {}
            }
        }

        for (int i = 0; i < networkSize; i++) {
            int id = i + 1;
            if (i < zeronodes) {
                miningPower = 0;
            } else {
                miningPower = avg_miningPower;
            }
            Node node = new Node(
                    id,
                    regionList.get(i),
                    miningPower,
                    SimulationConfiguration.ROUTING_TABLE,
                    SimulationConfiguration.CONSENSUS_CLASS,
                    SimulationConfiguration.PROPAGATION_CLASS,
                    churnNodes.get(i));
            Simulator.addNode(node);
        }
        // set miningpower for pools
        if (NetworkConfiguration.getUSEMINGPOOLS()) {
            int count = 0;
            HashSet<Integer> used = new HashSet<>();
            while (count < NetworkConfiguration.getPoolCount()) {
                int rand = simRng.nextInt(Simulator.getSimulatedNodes().size());
                if (!used.contains(rand)) {
                    if ((avg_miningPower > 0 && Simulator.getSimulatedNodes().get(rand).getMiningPower() > 0)
                            || avg_miningPower == 0) {
                        Simulator.getSimulatedNodes().get(rand).setMiningPower(
                                Math.round((NetworkConfiguration.getNetworkMiningPower() / 100)
                                        * NetworkConfiguration.getPoolProportion()[count]));
                        count++;
                        Simulator.getSimulatedNodes().get(rand).isMiningPool = true;
                        used.add(rand);
                    }
                }
            }
        }
        // connect nodes
        for (Node node : Simulator.getSimulatedNodes()) {
            node.joinNetwork(connectedMiners);
        }
        GossipsubRoutingTable.graph = null;
        // choose new highbandwidth connections
        if (Simulator.getSimulatedNodes().get(0).getPropagationProtocol() instanceof Cbr) {
            // TODO NOTE: dynamic neighbor connections.
            Cbr.categorizeConnectionsByBandwidth(Simulator.getSimulatedNodes());
        }
    }

    public static void startSimulation() {
        if (SimulationConfiguration.CHURN_ON) {
            for (Node node : Simulator.getSimulatedNodes()) {
                node.scheduleOfflineEvent();
            }
            Node.scheduleGenesisBlock(3600*1000);  // send teh genesis block after one day in churn setting
            Timer.putTask(new SampleNetworkStatTask());
        } else {
            Node.scheduleGenesisBlock(0);
        }
    }
}
