package simblock.adversary;

public class AdversaryCapability {
    /* Total number of nodes that can be corrupted */
    final int numCorruptableNodes;
    /* Total number of links that can be corrupted */
    final int numCorruptableLinks;
    /** Time in second it takes to corrupt budget of nodes and links */
    final double corruptionDelay;

    public AdversaryCapability(int nNode, int nLink, double corruptionDelay) {
        this.numCorruptableNodes = nNode;
        this.numCorruptableLinks = nLink;
        this.corruptionDelay = corruptionDelay;
    }

    /**
     * Corrupted nodes are reset for each new attack. Therefore, during attack
     * planning, we ignore attacked or corrupted nodes and always use
     * numCorruptableNodes and numCorruptableLinks as our budget.
     * @param knowledge
     * @return
     */
    @Deprecated
    public int getAttackBudgetForNode(AdversaryKnowledge knowledge) {
        return numCorruptableNodes 
                - knowledge.nodeCorruptionWip.size() 
                - knowledge.corruptedNodes.size();
    }

    @Deprecated
    public int getAttackBudgetForLink(AdversaryKnowledge knowledge) {
        return numCorruptableLinks 
                - knowledge.getNumLinks(knowledge.linkCorruptionWip)
                - knowledge.getNumLinks(knowledge.corruptedLinks);
    }
}
