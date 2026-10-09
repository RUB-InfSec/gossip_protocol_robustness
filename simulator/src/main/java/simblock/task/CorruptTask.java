package simblock.task;

import simblock.adversary.Adversary;

public class CorruptTask implements Task {
    protected int interval;  // in millisecond

    protected Adversary adversary;

    public CorruptTask(Adversary adversary, int corruptionDelayMS) {
        this.interval = corruptionDelayMS;
        this.adversary = adversary;
    }

    @Override
    public int getInterval() {
        return this.interval;
    }
    

    @Override
    public void run() {
    }
}
