package simblock.task;

import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.simulator.Simulator;
import simblock.simulator.Timer;

public class SampleNetworkStatTask implements Task {

    int interval = 10*60*1000; // Sample the network stats every 10 mins

    @Override
    public int getInterval() {
        return interval;
    }

    @Override
    public void run() {
        if (AbstractConsensusAlgo.isSimulationFinished()) {
            return;
        }
        Simulator.statsNumOnlineHonestNodes.add(Simulator.getSimulatedNodes().stream().filter(n->n.isOnline() && !n.isCorrupted()).toList().size());
        Timer.putTask(new SampleNetworkStatTask());
    }
}
