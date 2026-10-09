package simblock.node.transmission;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import simblock.block.Block;
import simblock.block.Rumor;
import simblock.block.RumorState;
import simblock.node.Node;
import simblock.node.propagation.AbstractPropagationProtocol;
import simblock.settings.NetworkConfiguration;
import simblock.simulator.Network;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.task.AbstractMessageTask;
import simblock.task.BlockMessageTask;
import simblock.task.Task;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SerialTransmissionTest {

    List<Node> createNodes(int num) {
        Simulator.reset();
        for (int i = 0; i < num; i++) {
            Node node = new Node(i + 1, i, 0,
                    "simblock.node.routing.GridRoutingTable",
                    "simblock.node.consensus.RumorSpreading",
                    "simblock.node.propagation.Push",
                    false);
            Simulator.addNode(node);
            AbstractPropagationProtocol spyProp = spy(node.getPropagationProtocol());
            doNothing().when(spyProp).handleReceivedMessage(any(AbstractMessageTask.class));
            node.setPropagationProtocol(spyProp);
        }
        for (Node n: Simulator.getSimulatedNodes()) {
            n.joinNetwork(false);
        }
        return Simulator.getSimulatedNodes();
    }

    @Test
    void testSerialTransmission() {
        List<Node> nodes = createNodes(4);
        Node node1 = nodes.get(0);
        Node node2 = nodes.get(1);
        Node node3 = nodes.get(2);
        Node node4 = nodes.get(3);

        int now = Timer.getCurrentTime();
        assertEquals(0, now);
        Rumor.endState = RumorState.EndState.ONE;
        Rumor rumor = new Rumor(node1, now);

        // mock the getLatency return 10 and 20
        long intervalNode1ToNode2 = 10;
        long intervalNode1ToNode3 = 20;
        long intervalNode2ToNode3 = 50;
        long intervalNode2ToNode4 = 60;

        try (MockedStatic<Network> mockedNetwork = mockStatic(Network.class)) {
            // set transmission delay = 0
            NetworkConfiguration.SIMULATE_REAL_NETWORK_LATENCY = false;

            // only define link latency for tests
            mockedNetwork.when(() -> Network.getEdgeLatency(node1.getRegion(), node2.getRegion())).thenReturn(intervalNode1ToNode2);
            mockedNetwork.when(() -> Network.getEdgeLatency(node1.getRegion(), node3.getRegion())).thenReturn(intervalNode1ToNode3);
            mockedNetwork.when(() -> Network.getEdgeLatency(node2.getRegion(), node3.getRegion())).thenReturn(intervalNode2ToNode3);
            mockedNetwork.when(() -> Network.getEdgeLatency(node2.getRegion(), node4.getRegion())).thenReturn(intervalNode2ToNode4);

            node1.getPropagationProtocol().addKnownMessage(rumor, node1);
            node1.getPropagationProtocol().sendBlock(rumor, Arrays.asList(node2, node3));

            Task task = Timer.runTask();
            assertInstanceOf(BlockMessageTask.class, task);
            assertEquals(node2, ((BlockMessageTask) task).getTo());
            assertEquals(intervalNode1ToNode2, Timer.getCurrentTime());

            node2.getPropagationProtocol().addKnownMessage(rumor, node2);
            node2.getPropagationProtocol().sendBlock(rumor, Arrays.asList(node3, node4));

            task = Timer.runTask();
            assertInstanceOf(BlockMessageTask.class, task);
            assertEquals(node3, ((BlockMessageTask) task).getTo());
            assertEquals(intervalNode1ToNode2 + intervalNode1ToNode3, Timer.getCurrentTime());

            task = Timer.runTask();
            assertInstanceOf(BlockMessageTask.class, task);
            assertEquals(node3, ((BlockMessageTask) task).getTo());
            assertEquals(intervalNode1ToNode2 + intervalNode2ToNode3, Timer.getCurrentTime());

            task = Timer.runTask();
            assertInstanceOf(BlockMessageTask.class, task);
            assertEquals(node4, ((BlockMessageTask) task).getTo());
            assertEquals(intervalNode1ToNode2 + intervalNode2ToNode3 + intervalNode2ToNode4, Timer.getCurrentTime());
        }
    }
}
