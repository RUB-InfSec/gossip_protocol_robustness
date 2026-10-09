package simblock.task;

import simblock.node.Node;
import simblock.block.AbstractMessage;
import simblock.block.Shred;
import static simblock.simulator.Network.computeTransmissionDelay;

public class ShredTask extends AbstractMessageTask {
    private Shred shred;
    private final int interval;

    public ShredTask(Node from, Node to, Shred s) {
        super(from, to);
        this.shred = s;

        this.interval = super.getInterval()
                + computeTransmissionDelay(s.size, this.getFrom().getRegion(), this.getTo().getRegion());
    }

    public Shred getShred() {
        return this.shred;

    }

    @Override
    public int getInterval() {
        return this.interval;
    }

    @Override
    public AbstractMessage getMessage() {
        return this.shred;
    }
}
