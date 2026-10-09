package simblock.adversary;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import simblock.node.Node;
import simblock.node.Link;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;

public class AdversaryTest {

    public static Adversary getGlobalKnowledgeCapability2Adversary() {
        AdversaryKnowledge knowledge = new AdversaryKnowledge(true);
        AdversaryCapability capability = new AdversaryCapability(2, 2, 0);
        AdversaryStrategy strategy = new NoneAdversaryStrategy();
        return new Adversary(capability, knowledge, strategy);
    }

    @BeforeAll
    public static void setupNetwork() {
        SimulationConfiguration.DEBUG_MODE = true;
        Simulator.reset();
        int networkSize = 6;
        for (int i = 0; i < networkSize; i++) {
            Node node = new Node(i+1, i, 0,
                    "simblock.node.routing.RandomGraphRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
        }
    }

    @BeforeEach
    public void resetEventQueue() {
        Timer.reset();
    }

    @Test
    public void testAttackNode() {
        List<Node> nodes = Simulator.getSimulatedNodes();

        AdversaryKnowledge knowledge = new AdversaryKnowledge(true);
        AdversaryCapability capability = new AdversaryCapability(2, 2, 0);
        AdversaryStrategy strategy = new NoneAdversaryStrategy();
        Adversary adversary;
        Node target;
        List<Object> targets;

        adversary = new Adversary(capability, knowledge, strategy);

        // attack and corrupt node 0 ok
        target = nodes.get(0);
        assertTrue(adversary.attackNode(target));
        assertEquals(0, knowledge.corruptedNodes.size());
        assertEquals(1, knowledge.nodeCorruptionWip.size());
        assertTrue(adversary.knowledge.isNodeAttacked(target));
        assertFalse(adversary.knowledge.isNodeCorrupted(target));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(1, knowledge.corruptedNodes.size());
        assertEquals(0, knowledge.nodeCorruptionWip.size());
        assertFalse(adversary.knowledge.isNodeAttacked(target));
        assertTrue(adversary.knowledge.isNodeCorrupted(target));

        // in a new attack, everything is reset
        target = nodes.get(2);
        assertTrue(adversary.attackNode(target));
        assertEquals(1, knowledge.corruptedNodes.size());
        assertEquals(1, knowledge.nodeCorruptionWip.size());
        assertTrue(adversary.knowledge.isNodeAttacked(target));
        assertFalse(adversary.knowledge.isNodeCorrupted(target));
        // new attack failed before previous one is completed
        assertFalse(adversary.attackNode(nodes.get(3)));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(1, knowledge.corruptedNodes.size());
        assertEquals(0, knowledge.nodeCorruptionWip.size());
        assertFalse(adversary.knowledge.isNodeAttacked(target));
        assertTrue(adversary.knowledge.isNodeCorrupted(target));

        // attack multiple nodes
        targets = Arrays.asList(nodes.get(0), nodes.get(1));
        assertTrue(adversary.attack(targets));
        assertEquals(1, knowledge.corruptedNodes.size());
        assertEquals(2, knowledge.nodeCorruptionWip.size());
        assertTrue(adversary.knowledge.isNodeAttacked(nodes.get(0)));
        assertTrue(adversary.knowledge.isNodeAttacked(nodes.get(1)));
        assertFalse(adversary.knowledge.isNodeCorrupted(nodes.get(0)));
        assertFalse(adversary.knowledge.isNodeCorrupted(nodes.get(1)));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(2, knowledge.corruptedNodes.size());
        assertEquals(0, knowledge.nodeCorruptionWip.size());
        assertFalse(adversary.knowledge.isNodeAttacked(nodes.get(0)));
        assertFalse(adversary.knowledge.isNodeAttacked(nodes.get(1)));
        assertTrue(adversary.knowledge.isNodeCorrupted(nodes.get(0)));
        assertTrue(adversary.knowledge.isNodeCorrupted(nodes.get(1)));

        // attack more than budget failed
        targets = Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(2));
        assertFalse(adversary.attack(targets));

    }

    @Test
    public void testAttackLink() {
        List<Node> nodes = Simulator.getSimulatedNodes();

        AdversaryKnowledge knowledge = new AdversaryKnowledge(true);
        AdversaryCapability capability = new AdversaryCapability(2, 2, 0);
        AdversaryStrategy strategy = new NoneAdversaryStrategy();
        Adversary adversary;
        Link target;
        List<Object> targets;

        adversary = new Adversary(capability, knowledge, strategy);

        // attack link 0->1 ok and corrupt ok
        target = new Link(nodes.get(0), nodes.get(1));
        assertTrue(adversary.attackLink(target));
        assertEquals(0, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(1, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertTrue(adversary.knowledge.isLinkAttacked(target));
        assertFalse(adversary.knowledge.isLinkCorrupted(target));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(1, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(0, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertFalse(adversary.knowledge.isLinkAttacked(target));
        assertTrue(adversary.knowledge.isLinkCorrupted(target));

        // in a new attack to Link 0->2, everything is reset
        target = new Link(nodes.get(0), nodes.get(2));
        assertTrue(adversary.attackLink(target));
        assertEquals(1, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(1, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertTrue(adversary.knowledge.isLinkAttacked(target));
        assertFalse(adversary.knowledge.isLinkCorrupted(target));
        // new attack failed before previous one is completed
        assertFalse(adversary.attack(Arrays.asList(nodes.get(3))));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(1, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(0, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertFalse(adversary.knowledge.isLinkAttacked(target));
        assertTrue(adversary.knowledge.isLinkCorrupted(target));
        
        // attack multiple links
        targets = Arrays.asList(new Link(nodes.get(1), nodes.get(2)), new Link(nodes.get(1), nodes.get(3)));
        assertTrue(adversary.attack(targets));
        assertEquals(1, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(2, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertTrue(adversary.knowledge.isLinkAttacked((Link) targets.get(0)));
        assertTrue(adversary.knowledge.isLinkAttacked((Link) targets.get(1)));
        assertFalse(adversary.knowledge.isLinkCorrupted((Link) targets.get(0)));
        assertFalse(adversary.knowledge.isLinkCorrupted((Link) targets.get(1)));
        assertTrue(adversary.isAttackInProgress());
        Timer.runTask();
        assertFalse(adversary.isAttackInProgress());
        assertEquals(2, knowledge.getNumLinks(knowledge.corruptedLinks));
        assertEquals(0, knowledge.getNumLinks(knowledge.linkCorruptionWip));
        assertFalse(adversary.knowledge.isLinkAttacked((Link) targets.get(0)));
        assertFalse(adversary.knowledge.isLinkAttacked((Link) targets.get(1)));
        assertTrue(adversary.knowledge.isLinkCorrupted((Link) targets.get(0)));
        assertTrue(adversary.knowledge.isLinkCorrupted((Link) targets.get(1)));

        // attack more than budget failed
        targets = Arrays.asList(new Link(nodes.get(1), nodes.get(2)), new Link(nodes.get(1), nodes.get(3)), new Link(nodes.get(0), nodes.get(1)));
        assertFalse(adversary.attack(targets));
    }
}
