package simblock.task;

import simblock.node.Node;

public abstract class AbstractChurnTask implements Task {
    protected Node node;
    protected int interval;

    public AbstractChurnTask(Node node, int duration) {
        this.node = node;
        interval = duration;
    }

    @Override
    public int getInterval() {
        return interval;
    }

    public abstract void run();
}
