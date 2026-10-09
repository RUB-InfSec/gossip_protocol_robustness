package simblock.adversary;

import static simblock.simulator.Simulator.simRng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import simblock.node.Node;
import simblock.node.Link;
import simblock.task.AbstractMessageTask;

public class RandomAdversaryStrategy extends AdversaryStrategy {

    public RandomAdversaryStrategy(AdversaryKnowledge knowledge) {
        super(knowledge);
    }

    @Override
    public List<Object> findTargets(AdversaryCapability capability) {
        List<Node> candidates = new ArrayList<>(knowledge.knownNodes);
        Collections.shuffle(candidates, simRng);

        ArrayList<Object> targets = new ArrayList<>();
        targets.addAll(candidates.subList(0, capability.numCorruptableNodes));

        // not deal with links for now
        int numLinks = capability.numCorruptableLinks;
        for (int i = 0; i < numLinks; i++) {
            int k = simRng.nextInt(candidates.size());
            Node from = candidates.get(k);  // does not matter whether the candidate is already a targeted node
            int l = simRng.nextInt(from.getNeighbors().size());
            Node to = (Node) from.getNeighbors().toArray()[l];
            targets.add(new Link(from, to));
        }
        return targets;
    }

    /**
     * Adaptive: first attack the nodes in the event queue, then randomly selected targets.
     */
    @Override
    public List<Object> findTargets(AdversaryCapability capability,
            AbstractMessageTask msgTask) {
        if (knowledge.global) {
//            Map<AbstractMessageTask, Long> pendingTasks = knowledge.getPendingTasks(capability.corruptionDelay);
            Map<AbstractMessageTask, Integer> pendingTasks = knowledge.getPendingTasks(0);
            List<Node> candidates = new ArrayList<>(knowledge.getCandidateNodes(pendingTasks).keySet());

            return deriveTargets(candidates, 
                capability.numCorruptableNodes, capability.numCorruptableLinks, 
                (rest, limit) -> {
                    Collections.shuffle(rest, simRng);
                    return rest.subList(0, limit);
                }, 
                node -> {
                    List<Link> links = new ArrayList<>();
                    for (Node inNode : knowledge.getConnections(node)) {
                        links.add(new Link(inNode, node));
                    }
                    return links;
                }
            );
        } else {
            throw new UnsupportedOperationException("Unimplemented method 'findTargets' for adaptive adversary with random strategy and non-global knowledge");
        }
    }
}