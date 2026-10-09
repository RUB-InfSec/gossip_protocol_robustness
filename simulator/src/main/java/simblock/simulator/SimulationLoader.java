package simblock.simulator;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import simblock.adversary.AdversaryKnowledge;
import simblock.graph.DijkstraShortestPath.Path;
import simblock.graph.centrality.BetweennessCentrality;
import simblock.graph.centrality.ClosenessCentrality;
import simblock.graph.centrality.EigenCentrality;
import simblock.node.Node;
import simblock.node.routing.GossipsubRoutingTable;
import simblock.node.routing.RandomGraphRoutingTable;
import simblock.settings.Config;
import simblock.settings.NetworkConfiguration;
import simblock.settings.SimulationConfiguration;

import java.io.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

import static simblock.simulator.Main.*;

/**
 * Save and load simulation network graph to save computation time.
 */
public class SimulationLoader {

    public static Map<Node, Map<Node, Path<Node>>> loadedShortestPaths = null;
    public static Map<Integer, Double> loadedEigenScores = null;

    public static JsonObject getNetworkGraphJsonObj() {
        JsonObject graph = new JsonObject();
        for (Node node : Simulator.getSimulatedNodes()) {
            JsonArray neighbors = new JsonArray();
            node.getRoutingTable().getNeighbors().forEach(n -> neighbors.add(n.getNodeID()));
            graph.add(String.valueOf(node.getNodeID()), neighbors);
        }
        return graph;
    }

    public static void prepareERGraphs(List<Integer> networkSizes, List<Integer> degrees) throws IOException {
        SimulationConfiguration.ROUTING_TABLE = "simblock.node.routing.RandomGraphRoutingTable";
        SimulationConfiguration.CONSENSUS_CLASS = "simblock.node.consensus.ProofOfWork";
        SimulationConfiguration.PROPAGATION_CLASS = "simblock.node.propagation.Push";
        int runs = 10;
        File graphJsonFile;
        for (Integer networkSize: networkSizes) {
            for (Integer degree: degrees) {
                NetworkConfiguration.DEGREE = degree;
                for (int i = 0; i < runs; i++) {
                    // construct network
                    NetworkConfiguration.setUSEMININGPOOLS(false);
                    Simulator.reset();
                    constructNetwork(networkSize, 0, false);

                    graphJsonFile = new File(OUT_FILE_URI.resolve(String.format("graph_%d_%d.%d.json", networkSize, degree, i+1)));
                    JsonFactory factory = new JsonFactory();
                    JsonGenerator gen = factory.createGenerator(graphJsonFile, JsonEncoding.UTF8);
                    ObjectMapper mapper = new ObjectMapper();
                    gen.setCodec(mapper);
                    gen.writeStartObject();
                    // save topology
                    JsonObject graph = getNetworkGraphJsonObj();
                    gen.writeFieldName("graph");
                    gen.writeObject(mapper.readTree(graph.toString()));

                    JsonArray regions = new JsonArray();
                    Simulator.getSimulatedNodes().forEach(node -> regions.add(node.getRegion()));
                    gen.writeFieldName("regions");
                    gen.writeObject(mapper.readTree(regions.toString()));

                    // save centrality scores
                    AdversaryKnowledge knowledge = new AdversaryKnowledge(true);
                    knowledge.restNodesForCentrality = Simulator.getSimulatedNodes();
                    ClosenessCentrality closeCent = new ClosenessCentrality(knowledge);
                    BetweennessCentrality betweenCent = new BetweennessCentrality(knowledge, closeCent);
                    gen.writeObjectFieldStart("shortestPaths");
                    for (Node n: closeCent.allShortestPaths.keySet()) {
                        JsonObject pathOfn = new JsonObject();
                        Map<Node, Path<Node>> shortestPaths = closeCent.allShortestPaths.get(n);
                        for (Node m: shortestPaths.keySet()) {
                            JsonObject path2m = new JsonObject();
                            path2m.addProperty("length", shortestPaths.get(m).getLength());
                            path2m.addProperty("paths", shortestPaths.get(m).printPath());
                            pathOfn.add(String.valueOf(m.getNodeID()), path2m);
                        }
                        gen.writeFieldName(String.valueOf(n.getNodeID()));
                        gen.writeObject(mapper.readTree(pathOfn.toString()));
                    }
                    gen.writeEndObject();
                    EigenCentrality eigenCent = new EigenCentrality(knowledge);
                    JsonObject eigenScoreJson = (new Gson()).toJsonTree(eigenCent.nodeScores).getAsJsonObject();
                    gen.writeFieldName("eigenvectorScores");
                    gen.writeObject(mapper.readTree(eigenScoreJson.toString()));
                    gen.writeEndObject();
                    gen.close();
                }
            }
        }
    }

    public static void loadNetworkConstruction(String graphFilePath, int networkSize, Config.NodeCentralityType centralityType) {
        if (!SimulationConfiguration.ROUTING_TABLE.toUpperCase().contains("RANDOM") &&
                !SimulationConfiguration.ROUTING_TABLE.toUpperCase().contains("GOSSIPSUB")) {
            throw new RuntimeException("Loading graph is not an option for non-random graph.");
        }
        if (SimulationConfiguration.ROUTING_TABLE.toUpperCase().contains("GOSSIPSUB")) {
            GossipsubRoutingTable.setConfig(NetworkConfiguration.DEGREE, simulatorConfig.network.gossipsub);
        }

        Graph<Integer, DefaultEdge> graph = null;
        List<Integer> regions = null;
        Map<Integer, Double> eigenScores = null;

        Simulator.reset();
        try (JsonReader reader = new JsonReader( new FileReader(graphFilePath))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String attr = reader.nextName();
                switch (attr) {
                    case "graph" -> {
                        graph = loadRandomGraph(reader);
                        RandomGraphRoutingTable.graph = graph;
                    }
                    case "regions" -> {
                        regions = readIntegerArray(reader);
                    }
                    case "eigenvectorScores" -> {
                        if (centralityType == Config.NodeCentralityType.EIGEN) {
                            loadedEigenScores = loadEigenvectorScores(reader);
                        } else {
                            reader.skipValue();
                        }
                    }
                    case "shortestPaths" -> {
                        // process later after the nodes are initialized
                        reader.skipValue();
                    }
                    default -> throw new RuntimeException("Network graph file contains unknown attribute: " + attr);
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Exception when reading graph file: " + e.getMessage());
        }

        if (graph == null || regions == null) {
            throw new RuntimeException("Missing information to load network configuration");
        }
        // Create nodes
        List<Boolean> churnNodes = makeRandomList(NetworkConfiguration.CHURN_RATE, networkSize);
        long avg_miningPower = calcMiningPower(networkSize);
        List<Node> nodes = new ArrayList<>(Collections.nCopies(graph.vertexSet().size(), null));
        for (Integer id : graph.vertexSet()) {
            Node node = new Node(
                    id,
                    regions.get(id-1),
                    avg_miningPower,
                    SimulationConfiguration.ROUTING_TABLE,
                    SimulationConfiguration.CONSENSUS_CLASS,
                    SimulationConfiguration.PROPAGATION_CLASS,
                    churnNodes.get(id-1));
            nodes.set(id-1, node);
        }
        for (Node node: nodes) {
            Simulator.addNode(node);
        }
        // Load the nodes to the shortest paths
        if (centralityType == Config.NodeCentralityType.CLOSENESS || centralityType == Config.NodeCentralityType.BETWEENNESS) {
            try (JsonReader reader = new JsonReader(new FileReader(graphFilePath))) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String attr = reader.nextName();
                    switch (attr) {
                        case "shortestPaths" -> {
                            loadedShortestPaths = loadShortestPath(reader);
                        }
                        default -> reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Exception when reading graph file: " + e.getMessage());
            }
        }
    }

    public static Graph<Integer, DefaultEdge> loadRandomGraph(JsonReader reader) throws IOException {
        Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        reader.beginObject();
        while (reader.hasNext()) {
            Integer nodeId = Integer.valueOf(reader.nextName());
            g.addVertex(nodeId);
            List<Integer> neighbors = readIntegerArray(reader);
            for (Integer n : neighbors) {
                g.addVertex(n);
                g.addEdge(nodeId, n);
            }
        }
        reader.endObject();
        return g;
    }

    public static Map<Node, Map<Node, Path<Node>>> loadShortestPath(JsonReader reader) throws IOException {
        List<Node> allNodes = Simulator.getSimulatedNodes();
        Map<Node, Map<Node, Path<Node>>> shortestPaths = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            Node m = allNodes.get(Integer.parseInt(reader.nextName())-1);
            Map<Node, Path<Node>> toOthers = new HashMap<>();
            reader.beginObject();
            while (reader.hasNext()) {
                Node n = allNodes.get(Integer.parseInt(reader.nextName())-1);
                reader.beginObject();
                Double length = 0.0;
                List<List<Node>> paths = null;
                while (reader.hasNext()) {
                    String attr = reader.nextName();
                    switch (attr) {
                        case ("length") -> length = reader.nextDouble();
                        case ("paths") -> {
                            String pathStr = reader.nextString();
                            if (pathStr.equals(Path.UNREACHABLE)) {
                                paths = null;
                            } else if (pathStr.isBlank()) {
                                paths = new ArrayList<>();
                            } else {    // multiple paths from m -> n
                                paths = new ArrayList<>();
                                for (String alterPathStr : pathStr.split("[|]")) {
                                    List<Node> newPath = new ArrayList<>();
                                    Arrays.stream(alterPathStr.split("->"))
                                            .skip(1)
                                            .map(el -> Integer.parseInt(el.replaceFirst("Node ", "")))
                                            .forEach(id -> newPath.add(allNodes.get(id - 1)));
                                    paths.add(newPath);
                                }
                            }
                        }
                        default -> throw new RuntimeException("Unknown attribute: " + attr);
                    }
                }
                toOthers.put(n, new Path(length, paths));
                reader.endObject();
            }
            reader.endObject();
            shortestPaths.put(m, toOthers);
        }
        reader.endObject();
        return shortestPaths;
    }

    public static Map<Integer, Double> loadEigenvectorScores(JsonReader reader) throws IOException {
        Map<Integer, Double> scores = new HashMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            scores.put(Integer.valueOf(reader.nextName()), reader.nextDouble());
        }
        reader.endObject();
        return scores;
    }

    public static List<Integer> readIntegerArray(JsonReader reader) throws IOException {
        List<Integer> listInt = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            listInt.add(reader.nextInt());
        }
        reader.endArray();
        return listInt;
    }
}
