package simblock.task;

import simblock.node.Node;
import simblock.node.consensus.AbstractConsensusAlgo;

import static simblock.simulator.Simulator.debugLog;

public class GoOfflineTask extends AbstractChurnTask {

    public GoOfflineTask(Node node, int duration) {
        super(node, duration);
    }

    @Override
    public void run() {
        if (!node.isOnline()) {
            return;
        }
        if (AbstractConsensusAlgo.isSimulationFinished()) {
            return;
        }
        debugLog(node + " going offline...");
        node.setOffline();
        node.scheduleOnlineEvent();
    }
}
