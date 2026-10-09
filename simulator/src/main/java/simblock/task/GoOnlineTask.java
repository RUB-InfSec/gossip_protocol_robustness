package simblock.task;

import simblock.node.Node;
import simblock.node.consensus.AbstractConsensusAlgo;

import static simblock.simulator.Simulator.debugLog;

public class GoOnlineTask extends AbstractChurnTask {

    public GoOnlineTask(Node node, int duration) {
        super(node, duration);
    }

    @Override
    public void run() {
        if (node.isOnline()) {
            return;
        }
        if (AbstractConsensusAlgo.isSimulationFinished()) {
            return;
        }
        debugLog(node + " going online...");
        node.setOnline();
        node.scheduleOfflineEvent();
    }
}
