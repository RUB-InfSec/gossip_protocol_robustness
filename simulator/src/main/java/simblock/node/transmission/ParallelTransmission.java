package simblock.node.transmission;

import simblock.task.AbstractMessageTask;
import simblock.task.EndTransmissionTask;
import simblock.task.IDataRequestHandler;
import simblock.simulator.Timer;

public class ParallelTransmission implements IMsgTransmission {
    private int currentlySending;

    /**
     * Sub-millisecond transmission time (in microseconds) left over from the
     * previous flow started on this node, carried forward instead of discarded
     * so that many small/fast messages whose individual delay rounds down to
     * 0ms still add up to the correct total time (see
     * {@link AbstractMessageTask#getTransmissionDelayMicros()}).
     */
    private long transmissionDelayMicrosCarry = 0L;

    @Override
    public void addTransmissionTask(IDataRequestHandler req) {
        assert this.currentlySending >= 0;
        this.currentlySending++;

        AbstractMessageTask sendDataTask = req.handleDataRequest(currentlySending);
        if (sendDataTask == null) {
            this.currentlySending--;
            return;
        }
        Timer.putTask(sendDataTask);
        // Free up this flow's share of the bandwidth as soon as it is done being
        // sent (transmission delay only), not when it finishes arriving at the
        // receiver (which additionally includes one-way propagation delay). The
        // remaining sub-millisecond microseconds are carried forward to the next
        // flow instead of being truncated away every time.
        long totalMicros = this.transmissionDelayMicrosCarry + sendDataTask.getTransmissionDelayMicros();
        int transmissionDelayMs = (int) (totalMicros / 1000);
        this.transmissionDelayMicrosCarry = totalMicros % 1000;
        Timer.putTask(new EndTransmissionTask(this, transmissionDelayMs));
    }

    @Override
    public void endMessageTransmission() {
        this.currentlySending--;
    }
}
