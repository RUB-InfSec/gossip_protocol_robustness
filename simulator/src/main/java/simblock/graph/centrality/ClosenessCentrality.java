package simblock.graph.centrality;

import java.util.*;

import simblock.node.Node;
import simblock.adversary.AdversaryKnowledge;
import simblock.graph.DijkstraShortestPath;
import simblock.graph.DijkstraShortestPath.Path;
import simblock.simulator.SimulationLoader;

import static simblock.simulator.Simulator.debugLog;

public class ClosenessCentrality extends Centrality {

    List<Node> networkLastCompute = null;
    public Map<Node, Map<Node, Path<Node>>> allShortestPaths;

    public ClosenessCentrality(AdversaryKnowledge knowledge) {
        super(knowledge);
        knowledge.restNodesForCentrality = knowledge.knownNodes;
        if (SimulationLoader.loadedShortestPaths == null) {
            computeAllShortestPaths();
        } else {
            networkLastCompute = knowledge.restNodesForCentrality;
            allShortestPaths = SimulationLoader.loadedShortestPaths;
        }
    }

    void computeAllShortestPaths() {
        if (!knowledge.isGlobal()) {
            throw new RuntimeException("Local adversary knowledge cannot compute closeness centrality");
        }
        if (knowledge.restNodesForCentrality.isEmpty()) {
            return;
        }
        // compute all the shortPaths for every node
        if (knowledge.restNodesForCentrality != networkLastCompute) {
            debugLog("Recompute the shortest paths for (sub)-network.");
            networkLastCompute = knowledge.restNodesForCentrality;
            allShortestPaths = new HashMap<>();
            for (Node n : knowledge.knownNodes) {
                List<Node> others = new ArrayList<>(knowledge.restNodesForCentrality);
                others.remove(n);
                allShortestPaths.put(n, findShortestPathToOthers(n, others));
            }
        }
    }

    /** The closeness centrality is the inverse of the sum of shortest paths to all other nodes */
    @Override
    public double compute(Node node) {
        if (knowledge.restNodesForCentrality.isEmpty()) {
            return 1.0 / Double.MAX_VALUE;
        }
        computeAllShortestPaths();
        return 1.0 / allShortestPaths.get(node).values().stream().map(path -> path.getLength()).mapToDouble(Double::doubleValue).sum();
    }

    public Map<Node, Path<Node>> findShortestPathToOthers(Node node, List<Node> others) {
        Set<Node> settled = new HashSet<Node>(knowledge.knownNodes);
        settled.removeAll(others);
        settled.remove(node);
        return DijkstraShortestPath.computeShortestPaths(node, knowledge.latencyMatrix, knowledge.knownNodes, settled);
    }

}
