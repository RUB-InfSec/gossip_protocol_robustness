package simblock.graph.centrality;

import simblock.adversary.AdversaryKnowledge;
import simblock.node.Node;

public abstract class Centrality {

    protected AdversaryKnowledge knowledge;

    public Centrality(AdversaryKnowledge kn) {
        knowledge = kn;
    }

    abstract public double compute(Node n);
}
