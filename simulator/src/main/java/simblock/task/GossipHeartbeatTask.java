package simblock.task;

import simblock.node.Node;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.propagation.Gossipsub;
import simblock.node.routing.GossipsubRoutingTable;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static simblock.simulator.Simulator.simRng;

public class GossipHeartbeatTask implements Task {

    int delay;
    protected static int interval;
    protected static int numRandomGossipNodes;

    public GossipHeartbeatTask() {
        this.delay = 0;
    }

    public GossipHeartbeatTask(int delay) {
        this.delay = delay;
    }

    public static void setHeartbeatInterval(int val, int numNodes) {
        interval = val;
        numRandomGossipNodes = numNodes;
    }

    @Override
    public int getInterval() {
        return interval + delay;
    }

    @Override
    public void run() {
        if (AbstractConsensusAlgo.isSimulationFinished()) {
            return;
        }
        List<Node> allnodes = new ArrayList<>(Simulator.getSimulatedNodes());
        for (Node node : allnodes) {
            Gossipsub gossipsub = (Gossipsub) node.getPropagationProtocol();
            List<Node> allAvailableNodes = new ArrayList<>(allnodes.stream().filter(n -> n.isOnline() && n != node).toList());
            Collections.shuffle(allAvailableNodes, simRng);
            gossipsub.sendBatchGossip(allAvailableNodes.subList(0, numRandomGossipNodes));

            // ----------- integrate peerScoreDecay task here to reduce the amount of tasks ----------
            ((GossipsubRoutingTable) node.getRoutingTable()).refreshScores();
        }
        // schedule the next heartbeat
        Timer.putTask(new GossipHeartbeatTask());
    }
}
