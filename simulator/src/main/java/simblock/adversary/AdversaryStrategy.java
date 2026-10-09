package simblock.adversary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import simblock.node.Node;
import simblock.node.Link;
import simblock.task.AbstractMessageTask;

public abstract class AdversaryStrategy {

    protected AdversaryKnowledge knowledge;

    public AdversaryStrategy(AdversaryKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    /** Static strategy */
    public abstract Collection<Object> findTargets(AdversaryCapability capability);

    /** Adaptive strategy */
    public abstract Collection<Object> findTargets(AdversaryCapability capability, AbstractMessageTask msgTask);

    /** Derive the target links according to a given node */
    @FunctionalInterface
    public interface FindLinksTo {
        List<Link> apply(Node node);
    }

    /** Derive a list of ranked remaining nodes from the `rest` list to complement the target candidates */
    @FunctionalInterface
    public interface FindRemainingNodes {
        List<Node> apply(List<Node> rest, int limit);
    }

    /**
     * Given a list of ranked candidates, the budget of target nodes and links,
     * return the first nNodes nodes, then for the remaining nodes, return its
     * links according to the findLinksTo function.
     * If the ranked candidates are less than the node budget, complement the
     * candidate list according to the findRemainingNodes function.
     * @param candidates ranked candidates
     * @param nNodes budget of target nodes
     * @param nLinks budget of target links
     * @param findRemainingNodes function that return nodes except for the candidates to fill the node budget
     * @param findLinksTo function that return links to a given node
     * @return a list of targets to corrupt
     */
    List<Object> deriveTargets(List<Node> candidates, int nNodes, int nLinks, FindRemainingNodes findRemainingNodes, FindLinksTo findLinksTo) {
        if (knowledge == null) {
            return Collections.emptyList();
        }
        List<Object> targets = new ArrayList<Object>();
        // if (ranked) candidates are less than the budget (e.g. derived from the pending tasks), add nodes based on static strategy
        if (candidates.size() < nNodes) { 
            List<Node> rest = new ArrayList<>(knowledge.knownNodes);
            rest.removeAll(candidates);
            candidates.addAll(findRemainingNodes.apply(rest, nNodes - candidates.size()));
            // rankedNodes.addAll(iterativelyRankNodes(unranked, knowledge, nNodes-rankedNodes.size()));
        }
        targets.addAll(candidates.subList(0, nNodes));
        /* NOTE: ignore link budget
        if (nLinks == 0) {
            return targets;
        }
        List<Link> targetedLink = new ArrayList<>();
        // TODO if candidates.size() < nNodes
        outer:
        for (Node node : candidates.subList(nNodes, candidates.size())) {
            for (Link link : findLinksTo.apply(node)) {
                // if the corresponding node of the link is not already a target
                if (!targets.contains(link.getFrom())) {    
                    targetedLink.add(link);
                    if (targetedLink.size() == nLinks) {
                        break outer;
                    }
                }
            }
        }
        targets.addAll(targetedLink);
        */
        return targets;
    }
}

