package simblock.graph.centrality;

import org.jgrapht.alg.scoring.EigenvectorCentrality;
import org.jgrapht.graph.DefaultEdge;
import simblock.adversary.AdversaryKnowledge;
import simblock.node.Node;
import simblock.node.routing.RandomGraphRoutingTable;
import simblock.simulator.SimulationLoader;

import java.util.Map;

public class EigenCentrality extends Centrality {

    EigenvectorCentrality<Integer, DefaultEdge> centrality = null;
    public Map<Integer, Double> nodeScores;

    public EigenCentrality(AdversaryKnowledge knowledge) {
        super(knowledge);
        if (SimulationLoader.loadedEigenScores == null) {
            updateNodeScores();
        } else {
            nodeScores = SimulationLoader.loadedEigenScores;
        }
    }

    void updateNodeScores() {
        // late initialization
        if (centrality == null) {
            // TODO for dynamic topology this will be a subgraph in adaptive strategy.
            centrality = new EigenvectorCentrality<>(RandomGraphRoutingTable.graph);
            nodeScores = centrality.getScores();
        }
    }

    @Override
    public double compute(Node n) {
        return nodeScores.get(n.getNodeID());
    }
}
