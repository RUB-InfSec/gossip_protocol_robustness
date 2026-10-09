package simblock.adversary;

import static simblock.simulator.Simulator.debugLog;

import java.util.*;
import java.util.stream.Collectors;

import simblock.node.Node;
import simblock.node.Link;
import simblock.settings.Config.AdversaryOpt;
import simblock.settings.Config.AdversaryStrategyType;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.task.AbstractMessageTask;
import simblock.task.CorruptLinkTask;
import simblock.task.CorruptTask;
import simblock.task.CorruptNodeTask;

public class Adversary {
    AdversaryCapability capability;
    AdversaryKnowledge knowledge;
    AdversaryStrategy strategy;

    private boolean adaptive;

    boolean attackInProgress = false;

    public Adversary(AdversaryOpt advOpt, int numPublicNodes) {
        adaptive = advOpt.adaptive;
        knowledge = new AdversaryKnowledge(advOpt.globalKnowledge, numPublicNodes);
        double corruptionDelay = adaptive ? advOpt.corruptionDelayS : 0;    // in millisecond
        capability = new AdversaryCapability(
            (int) Math.ceil(advOpt.pFailedNodes * Simulator.getSimulatedNodes().size()),
            (int) Math.ceil(advOpt.pFailedLinks * knowledge.totalLinks),
            corruptionDelay);
        try {
            String strategyClassName = "simblock.adversary." + advOpt.strategy.name().substring(0,1) + advOpt.strategy.name().substring(1).toLowerCase() + "AdversaryStrategy";
            strategy = (AdversaryStrategy) Class.forName(strategyClassName).getConstructor(AdversaryKnowledge.class).newInstance(knowledge);
            if (advOpt.strategy == AdversaryStrategyType.NONE) {
                adaptive = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Adversary(AdversaryCapability capability, AdversaryKnowledge knowledge, AdversaryStrategy strategy) {
        this.capability = capability;
        this.knowledge = knowledge;
        this.strategy = strategy;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public boolean isAttackInProgress() {
        return attackInProgress;
    }

    public void setAttackDone() {
        attackInProgress = false;
    }

    public void staticSetup() {
        if (adaptive) {
            return;
        }
        this.attack(strategy.findTargets(capability));
        // deploy static attack before the simulation starts
        while (Timer.hasNextTask()) {
            Timer.runTask();
        }
    }

    public void checkAttackOpportunity(AbstractMessageTask msgTask) {
//        knowledge.addSeenMessage(msgTask);
//         for (Node node: knowledge.getNodes2Release()) {
//             releaseNode(node);
//         }
        Collection<Object> targets = strategy.findTargets(capability, msgTask);
        debugLog("Attacker locates targets: " + targets);
        this.attack(targets);
    }

    public boolean attackNode(Node target) {
        return attack(new ArrayList<Object>(Arrays.asList(target)));
    }

    public boolean attackLink(Link target) {
        return attack(new ArrayList<Object>(Arrays.asList(target)));
    }

    public boolean attack(Collection<Object> targets) {
        if (attackInProgress) {
            debugLog("Previous attack has not been fully deployed.");
            return false;
        }
        attackInProgress = true;
        List<Node> nodes = targets.stream().filter(t -> t instanceof Node).map(t -> (Node) t).collect(Collectors.toList());
        if (nodes.size() > capability.numCorruptableNodes) {
            System.err.printf("[Error] targets size %d exceed budget of %d nodes\n", nodes.size(), capability.numCorruptableNodes);
            return false;
        }
        scheduleAttackNodes(nodes);
        /* NOTE ignore links
        List<Link> links = targets.stream().filter(t -> t instanceof Link).map(t -> (Link) t).collect(Collectors.toList());
        if (links.size() > capability.numCorruptableLinks) {
            debugLog(String.format("[Error] targets size %d exceed budget of %d links", links.size(), capability.numCorruptableLinks));
            return false;
        }
        scheduleAttackLinks(links);
        */
        return true;
    }
    
    private void scheduleAttackNodes(List<Node> nodes) {
        if (!nodes.isEmpty()) {
            debugLog("Attack nodes: " + nodes);
            CorruptTask task = new CorruptNodeTask(this, nodes, (int) (capability.corruptionDelay * 1000));
            Timer.putTask(task);
            knowledge.nodeCorruptionWip.addAll(nodes);
        }
    }

    private void scheduleAttackLinks(List<Link> links) {
        if (!links.isEmpty()) {
            debugLog("Attack links: " + links);
            CorruptLinkTask task = new CorruptLinkTask(this, links, (int) (capability.corruptionDelay * 1000));
            Timer.putTask(task);
            for (Link link: links) {
                HashSet<Node> toNodes = knowledge.linkCorruptionWip.getOrDefault(link.getFrom(), new HashSet<Node>());
                toNodes.add(link.getTo());
                knowledge.linkCorruptionWip.putIfAbsent(link.getFrom(), toNodes);
            }
        }
    }

    // Release all attacked nodes
    public void releaseNodes() {
        List<Node> nodes = new ArrayList<>(knowledge.corruptedNodes);
        nodes.addAll(knowledge.nodeCorruptionWip);
        for (Node node: nodes) {
            releaseNode(node);
        }
    }

    // Release all attacked links
    public void releaseLinks() {
        knowledge.corruptedLinks.clear();
        knowledge.linkCorruptionWip.clear();
        // debugLog("------ All links released.");
    }

    /**
     * Release the node no matter the node is already corrupted or still under attack
     * @param n targeted Node
     */
    public void releaseNode(Node n) {
        n.resetCorrupted();
        if (knowledge.corruptedNodes.contains(n)) {
            knowledge.corruptedNodes.remove(n);
            debugLog("------ " + n + " released.");
        }
        knowledge.nodeCorruptionWip.remove(n);
    }

    public void releaseLink(Link link) {
        if (knowledge.linkCorruptionWip.containsKey(link.getFrom())) {
            knowledge.linkCorruptionWip.get(link.getFrom()).remove(link.getTo());
        }
        HashSet<Node> toNodes = knowledge.corruptedLinks.getOrDefault(link.getFrom(), new HashSet<Node>());
        if (toNodes.contains(link.getTo())) {
            toNodes.remove(link.getTo());
            // debugLog("------ " + link + " released.");
        }
    }

    public void setNodeCorrupted(Node n) {
        n.setCorrupted();
        knowledge.corruptedNodes.add(n);
        debugLog("XXXXXX " + n + " corrupted.");
    }

    public void setLinkCorrupted(Link link) {
        HashSet<Node> toNodes = knowledge.corruptedLinks.getOrDefault(link.getFrom(), new HashSet<Node>());
        toNodes.add(link.getTo());
        knowledge.corruptedLinks.putIfAbsent(link.getFrom(), toNodes);
        debugLog("XXXXXX " + link + " corrupted.");
    }
}
