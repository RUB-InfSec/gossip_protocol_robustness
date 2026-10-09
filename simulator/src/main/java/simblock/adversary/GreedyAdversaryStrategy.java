package simblock.adversary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simblock.block.AbstractMessage;
import simblock.graph.centrality.Centrality;
import simblock.graph.centrality.ClosenessCentrality;
import simblock.node.Node;
import simblock.node.Link;
import simblock.settings.SimulationConfiguration;
import simblock.task.AbstractMessageTask;

public class GreedyAdversaryStrategy extends AdversaryStrategy {

    Centrality centrality;

    public GreedyAdversaryStrategy(AdversaryKnowledge knowledge) {
        super(knowledge);
        try {
            centrality = (Centrality) Class.forName(SimulationConfiguration.CENTRALITY_CLASS).getConstructor(
                    AdversaryKnowledge.class).newInstance(knowledge);
        } catch (Exception e) {
            System.err.println("Failed to reflect on class: " + SimulationConfiguration.CENTRALITY_CLASS);
            e.printStackTrace();
        }
    }

    /**
     * Oblivious (static): find the most important nodes in general
     */
    @Override
    public List<Object> findTargets(AdversaryCapability capability) {
        List<Node> allNodes = new ArrayList<>(knowledge.knownNodes);
        List<Node> rankedNodes = rankNodes(allNodes);
        return this.deriveTargets(rankedNodes,
                capability.numCorruptableNodes,
                capability.numCorruptableLinks,
                this::iterativelyRankNodes,
                node -> {
                    List<Link> links = new ArrayList<>();
                    for (Node neighbor : knowledge.getConnections(node)) {
                        links.add(new Link(neighbor, node));
                    }
                    return links;
                });
    }

    /**
     * TODO msgTask not used??!
     * TODO greedy strategy for Chunk Gossip?
     */
    @Override
    public List<Object> findTargets(AdversaryCapability capability,
            AbstractMessageTask msgTask) {
        if (knowledge.global) {
//            Map<AbstractMessageTask, Integer> pendingTasks = knowledge.getPendingTasks(capability.corruptionDelay);
            // assuming that the adversary only knows the list of nodes who will receive a message but not when
            Map<AbstractMessageTask, Integer> pendingTasks = knowledge.getPendingTasks(0);
            Map<Node, Map<AbstractMessage, Integer>> candidates = knowledge.getCandidateNodes(pendingTasks);
            // Rank the importance of candidates according to their `closeness
            // centrality`: for each candidate, sum up the shortest path to the
            // rest of the network.
//            List<Node> rankedCandidates = this.rankNodesClosenessCentrality(candidates);
            List<Node> rankedCandidates = this.rankNodesAndCntMsg(candidates);

            // search from the pending tasks for target links to the most important nodes.
            return this.deriveTargets(rankedCandidates,
                    capability.numCorruptableNodes,
                    capability.numCorruptableLinks,
//                    this::iterativelyRankNodes,
                    (rest, limit) -> rankNodes(rest).subList(0, limit),
                    node -> {
                        List<Link> links = new ArrayList<>();
                        for (AbstractMessageTask task : pendingTasks.keySet()) {
                            if (task.getTo() == node) {
                                links.add(new Link(task.getFrom(), node));
                            }
                        }
                        // TODO if there is not enough pending tasks for the target
                        return links;
                    });
        } else {
            throw new UnsupportedOperationException("not implemented adaptive 'findTargets' for local knowledge");
        }
    }

    List<Node> rankNodes(List<Node> nodes) {
        HashMap<Node, Double> results = new HashMap<>();
        knowledge.restNodesForCentrality = new ArrayList<>(nodes);
        for (Node node : nodes) {
            results.put(node, centrality.compute(node));
        }
        List<Node> rankedNodes = new ArrayList<>(results.keySet());
        // descending order
        Collections.sort(rankedNodes,
                (k1, k2) -> results.get(k2).compareTo(results.get(k1)));
        return rankedNodes;
    }

    /**
     * Rank nodes iteratively by comparing their closeness centrality. Once the
     * highest ranked node is found, remove it from the list and rank the rest
     * again until derive the top `candidateSize` ranked nodes.
     * @param nodes
     * @return a list of ranked nodes
     */
    List<Node> iterativelyRankNodes(List<Node> nodes, int limit) {
        List<Node> rankdedNodes = new ArrayList<>();
        while (rankdedNodes.size() < Math.min(limit, nodes.size())) {
            List<Node> rest = new ArrayList<>(nodes);
            rest.removeAll(rankdedNodes);
            double maxVal = Double.MIN_VALUE;
            Node highestRankedNode = null;
            knowledge.restNodesForCentrality = nodes;
            for (Node node: rest) {
                double val = centrality.compute(node);
                if (val > maxVal) {
                    maxVal = val;
                    highestRankedNode = node;
                }
            }
            rankdedNodes.add(highestRankedNode);
        }
        return rankdedNodes;
    }

    /**
     * Rank the candidates according to 1) the number of messages a node receives, 2) their centrality score.
     *
     * @param candidates
     * @return ranked nodes with descending order
     */
    List<Node> rankNodesAndCntMsg(Map<Node, Map<AbstractMessage, Integer>> candidates) {
        HashMap<Node, Double> centralityScores = new HashMap<>();
        knowledge.restNodesForCentrality = new ArrayList<>(candidates.keySet());
        for (Node node : candidates.keySet()) {
            centralityScores.put(node, centrality.compute(node));
        }
        List<Node> rankedNodes = new ArrayList<>(centralityScores.keySet());
        Collections.sort(rankedNodes, (k1, k2) -> {
            if (candidates.get(k1).size() == candidates.get(k2).size()) {
                return centralityScores.get(k2).compareTo(centralityScores.get(k1));
            } else {
                return candidates.get(k2).size() - candidates.get(k1).size();
            }
        });
        return rankedNodes;
    }



    /**
     * Compute the closeness centrality for each candidate considering a
     * subnetwork of nodes who have not yet received a certain message that
     * would be propagated by the candidate with a certain message delay. Then
     * rank the candidates first according to how many messages are
     * simultaneously scheduled to them, then provided equal importnace, their
     * total closeness centrality.
     *
     * @param candidates
     * @return rankded nodes with descending order
     */
    List<Node> rankNodesClosenessCentrality(Map<Node, Map<AbstractMessage, Integer>> candidates) {
        Centrality centrality = new ClosenessCentrality(knowledge);
        Map<Node, List<Double>> nodeCentralitiesInverse = new HashMap<>();
        for (Node node : candidates.keySet()) {
            List<Double> closenessCentralitiesInv = new ArrayList<>();
            Map<AbstractMessage, Integer> scheduledMsgs = candidates.get(node);
            for (AbstractMessage msg : scheduledMsgs.keySet()) {
                List<Node> unreachedNodes = knowledge.getUnreachedNodes(msg);
                knowledge.restNodesForCentrality = unreachedNodes;
                double centralityInv = scheduledMsgs.get(msg)
                        + 1.0/centrality.compute(node);
                closenessCentralitiesInv.add(centralityInv);
            }
            nodeCentralitiesInverse.put(node, closenessCentralitiesInv);
        }
        // smaller centrality inverse wins (ascending)
        List<Node> rankedNodes = new ArrayList<>(nodeCentralitiesInverse.keySet());
        Collections.sort(rankedNodes, (k1, k2) -> (new ClosenessCentralityInverseComparator())
                .compare(nodeCentralitiesInverse.get(k1), nodeCentralitiesInverse.get(k2)));
        return rankedNodes;
    }

    /**
     * First compare the size of the list, the more elements the more
     * centrality; if both have the same amount of elements, compare the total
     * sum of all elements, the least sum the more centrality.
     */
    public class ClosenessCentralityInverseComparator implements Comparator<List<Double>> {
        // return negative is measure1 is less than measure2
        public int compare(List<Double> measure1, List<Double> measure2) {
            if (measure1.size() == measure2.size()) {
                return (int) (measure1.stream().mapToDouble(Double::doubleValue).sum()
                        - measure2.stream().mapToDouble(Double::doubleValue).sum());
            }
            return measure2.size() - measure1.size();
        }
    }
}
