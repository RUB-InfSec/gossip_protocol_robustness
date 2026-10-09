package simblock.task;

import java.util.List;

import simblock.adversary.Adversary;
import simblock.node.Node;

public class CorruptNodeTask extends CorruptTask {

    private List<Node> targetedNodes;

    public CorruptNodeTask(Adversary adv, List<Node> nodes, int corruptionDelayMS) {
        super(adv, corruptionDelayMS);
        this.targetedNodes = nodes;
    }

    public List<Node> getTargetedNodes() {
        return targetedNodes;
    }
    
    @Override
    public void run() {
        adversary.releaseNodes();
        for (Node node: targetedNodes) {
            adversary.setNodeCorrupted(node);
        }
        adversary.setAttackDone();
    }
}
