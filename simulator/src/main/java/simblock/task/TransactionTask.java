package simblock.task;

import simblock.block.AbstractMessage;
import simblock.block.Transaction;
import simblock.node.Node;

import static simblock.simulator.Network.computeTransmissionDelay;
import static simblock.settings.SimulationConfiguration.TRANSACTION_SIZE;

/*
 * Task that models  the sending of a Transaction
 */
public class TransactionTask extends AbstractMessageTask {
    private Transaction transaction;
    private final int interval;
    private final int size = TRANSACTION_SIZE; // in byte

    /**
     * creator used for sending transactions
     */
    public TransactionTask(Node from, Node to, Transaction transaction) {
        super(from, to);
        this.transaction = transaction;
        this.interval = super.getInterval() + computeTransmissionDelay(this.size, from.getRegion(), to.getRegion());
    }

    /**
     *
     * creator used for crating new transactions after a delay
     */
    public TransactionTask(Node from, Node to, Transaction transaction, int delay) {
        super(from, to);
        this.transaction = transaction;
        if (from == null) {
            // Schedule to initiate a new transaction
            this.interval = delay;
        } else {
            this.interval = super.getInterval() + computeTransmissionDelay(this.size, from.getRegion(), to.getRegion())
                    + delay;
        }

    }

    @Override
    public int getInterval() {
        return this.interval;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    @Override
    public AbstractMessage getMessage() {
        return this.transaction;
    }
}
