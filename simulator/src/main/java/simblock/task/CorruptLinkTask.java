package simblock.task;

import java.util.List;

import simblock.adversary.Adversary;
import simblock.node.Link;

public class CorruptLinkTask extends CorruptTask {
    private List<Link> targetedLinks;

    public CorruptLinkTask(Adversary adv, List<Link> links, int corruptionDelayS) {
        super(adv, corruptionDelayS);
        this.targetedLinks = links;
    }

    public List<Link> getTargetedLinks() {
        return targetedLinks;
    }
        
    @Override
    public void run() {
        adversary.releaseLinks();
        for (Link link: targetedLinks) {
            adversary.setLinkCorrupted(link);
        }
        adversary.setAttackDone();
    }
}
