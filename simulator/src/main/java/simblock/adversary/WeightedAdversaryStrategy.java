package simblock.adversary;

import static simblock.simulator.Simulator.simRng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import simblock.node.Node;
import simblock.node.Link;
import simblock.task.AbstractMessageTask;

public class WeightedAdversaryStrategy extends AdversaryStrategy {

    public WeightedAdversaryStrategy(AdversaryKnowledge knowledge) {
        super(knowledge);
    }

    @Override
    public List<Object> findTargets(AdversaryCapability capability) {
        List<Node> candidates = new ArrayList<>(knowledge.knownNodes);
        candidates.sort((a, b) -> Float.compare(b.getStakeProportion(), a.getStakeProportion()));

        ArrayList<Object> targets = new ArrayList<>();
        targets.addAll(candidates.subList(0, capability.numCorruptableNodes));

        int numLinks = capability.numCorruptableLinks;
        for (int i = 0; i < numLinks; i++) {
            // not deal with links for now
        }
        return targets;
    }

    /**
     * Adaptive: first attack the nodes in the event queue, then randomly selected targets.
     */
    @Override
    public List<Object> findTargets(AdversaryCapability capability,
            AbstractMessageTask msgTask) {
        throw new UnsupportedOperationException("Unimplemented method 'findTargets' for adaptive adversary with weighted strategy and non-global knowledge");
    }
}