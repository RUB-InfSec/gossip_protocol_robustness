package simblock.node.transmission;

import java.util.ArrayList;

import simblock.task.AbstractMessageTask;
import simblock.task.EndTransmissionTask;
import simblock.task.IDataRequestHandler;
import simblock.simulator.Timer;

public class SerialTransmission implements IMsgTransmission {
    /**
     * A pending list of requests to send data
     */
    private final ArrayList<IDataRequestHandler> requests = new ArrayList<>();

    /**
     * Indicate wether a message is in transmission
     */
    private boolean transmissionInProgress;

    /**
     * Sub-millisecond transmission time (in microseconds) left over from the
     * previous message, carried forward instead of discarded so that many
     * small/fast messages whose individual delay rounds down to 0ms still add
     * up to the correct total time on this link (see
     * {@link AbstractMessageTask#getTransmissionDelayMicros()}).
     */
    private long transmissionDelayMicrosCarry = 0L;

    public SerialTransmission() {
    }

    /**
     * Add a new message in the queue to send. If there is currently no message
     * in transmission, send the next one.
     */
    @Override
    public void addTransmissionTask(IDataRequestHandler msg) {
        this.requests.add(msg);
        if (!this.transmissionInProgress) {
            this.sendNextMessage();
        }
    }

    /**
     * Starts the next transmission when the current transmission ends.
     */
    @Override
    public void endMessageTransmission() {
        this.sendNextMessage();
    }

    private void sendNextMessage() {
        if (this.requests.isEmpty()) {
            transmissionInProgress = false;
            return;
        }
        while (this.requests.size() > 0) {
            IDataRequestHandler req = this.requests.get(0);
            AbstractMessageTask sendDataTask = req.handleDataRequest(1);
            if (sendDataTask == null) {
                // debugLog(String.format("Node %d skipped sendMessage to node %d. Received same
                // message after scheduling.", this.selfNode.getNodeID(), to.getNodeID()));
                this.requests.remove(0);
                if (this.requests.isEmpty()) {
                    transmissionInProgress = false;
                }
                continue;
            }
			this.requests.remove(0);
			this.transmissionInProgress = true;
            Timer.putTask(sendDataTask);
            // Free this link as soon as the message is done being sent (transmission
            // delay only), not when it finishes arriving at the receiver (which
            // additionally includes one-way propagation delay). The remaining
            // sub-millisecond microseconds are carried forward to the next message
            // instead of being truncated away every time.
            long totalMicros = this.transmissionDelayMicrosCarry + sendDataTask.getTransmissionDelayMicros();
            int transmissionDelayMs = (int) (totalMicros / 1000);
            this.transmissionDelayMicrosCarry = totalMicros % 1000;
            Timer.putTask(new EndTransmissionTask(this, transmissionDelayMs));
            break;
        }
    }

}
