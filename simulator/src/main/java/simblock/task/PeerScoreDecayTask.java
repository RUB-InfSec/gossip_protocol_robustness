package simblock.task;

import simblock.node.Node;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.routing.GossipsubRoutingTable;
import simblock.simulator.Timer;

public class PeerScoreDecayTask implements Task {

    protected Node node;
    protected static int interval;

    public static void setDecayInterval(int val) {
        interval = val;
    }

    public PeerScoreDecayTask(Node node) {
        this.node = node;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public void run() {
        if (AbstractConsensusAlgo.isSimulationFinished()) {
            return;
        }
        ((GossipsubRoutingTable) node.getRoutingTable()).refreshScores();
        Timer.putTask(new PeerScoreDecayTask(node));
    }
}
