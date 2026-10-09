package simblock.graph.centrality;

import java.util.List;
import java.util.Map;

import simblock.adversary.AdversaryKnowledge;
import simblock.graph.DijkstraShortestPath;
import simblock.node.Node;

public class BetweennessCentrality extends Centrality {

    ClosenessCentrality closenessCentrality;

    public BetweennessCentrality(AdversaryKnowledge knowledge) {
        super(knowledge);
        closenessCentrality = new ClosenessCentrality(knowledge);
    }

    public BetweennessCentrality(AdversaryKnowledge knowledge, ClosenessCentrality closeness) {
        super(knowledge);
        closenessCentrality = closeness;
    }


    @Override
    public double compute(Node node) {
        closenessCentrality.computeAllShortestPaths();
        List<Node> networkLastCompute = closenessCentrality.networkLastCompute;
        Map<Node, Map<Node, DijkstraShortestPath.Path<Node>>> allShortestPaths = closenessCentrality.allShortestPaths;

        int countInShortestPath = 0;
        for (int i = 0; i < networkLastCompute.size(); i++) {
            Node m = networkLastCompute.get(i);
            if (m == node) {
                continue;
            }
            for (int j = i+1; j < networkLastCompute.size(); j++) {
                Node n = networkLastCompute.get(j);
                if (n == node) {
                    continue;
                }
                if (allShortestPaths.get(m).get(n).containNode(node)) {
                    countInShortestPath++;
                }
            }
        }
        return countInShortestPath;
    }

}
