package simblock.graph.centrality;

import simblock.adversary.AdversaryKnowledge;
import simblock.node.Node;

public class DegreeCentrality extends Centrality {

    public DegreeCentrality(AdversaryKnowledge knowledge) {
        super(knowledge);
    }

    @Override
    public double compute(Node n) {
        return knowledge.getConnections(n).size();
    }
    
}
