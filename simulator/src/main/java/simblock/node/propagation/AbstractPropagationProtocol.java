package simblock.node.propagation;

import java.util.*;
import java.util.stream.Collectors;

import simblock.node.Node;
import simblock.node.transmission.IMsgTransmission;
import simblock.block.Block;
import simblock.block.Transaction;
import simblock.block.AbstractMessage;
import simblock.simulator.Simulator;
import simblock.task.*;

import static simblock.settings.NetworkConfiguration.SIMULATE_REAL_NETWORK_LATENCY;
import static simblock.settings.SimulationConfiguration.*;

import simblock.simulator.Timer;
import static simblock.simulator.Simulator.debugLog;

/**
 * Abstact propagation protocl class, that implements all basic propagation
 * behavior
 */
abstract public class AbstractPropagationProtocol {
    protected final Node selfNode;

    protected boolean useCBR;

    protected IMsgTransmission msgTransMechanism;
    /**
     * local mempool
     */
    protected final HashSet<Transaction> mempool = new HashSet<>();

    /**
     * all messages received by the node and record the sender node.
     * TODO NOTE: limit buffer size
     */
    protected HashMap<AbstractMessage, HashSet<Node>> knownMessages = new HashMap<>();

    /**
     * Simulated processing time of (all??) tasks in milliseconds.
     */
    private final int processingTime = 2;

    public AbstractPropagationProtocol(Node node) {
        this.selfNode = node;
    }

    /**
     * Simulated time for local process of the message
     * @return
     */
    public int getProcessingTime() {
        if (SIMULATE_REAL_NETWORK_LATENCY) {
            return processingTime;
        }
        return 0;   // simulate with rounds
    }

    /**
     * Flag to signal the use of compact blocks of any form in propagation.
     * default=false
     *
     */
    public boolean useCBR() {
        return this.useCBR;
    }

    public int mempoolSize() {
        return this.mempool.size();
    }

    /**
     * Return true if the node already knows about a given msg.
     * @param node
     * @param msg
     * @return
     */
    public boolean knowsMessage(Node node, AbstractMessage msg) {
        return this.knownMessages.get(msg).contains(node);
    }

    /**
     * Add the message `m` to knownMessage set as well as from whom has the node
     * heard from the message.
     * In this way the node can avoid sending messages to these nodes.
     * Message is only added to the set only when the full message is received.
     */
    public void addKnownMessage(AbstractMessage msg, Node n) {
        HashSet<Node> nodes = this.knownMessages.getOrDefault(msg, new HashSet<>());
        nodes.add(n);
        this.knownMessages.putIfAbsent(msg, nodes);
    }

    /**
     * Collect a given amount of transactions from the mempool.
     */
    public HashSet<Transaction> collectMempoolTx(int numTx) {
        return new HashSet<Transaction>(this.mempool.stream().limit(numTx).collect(Collectors.toSet()));
    }


    /**
     * Abstarct propagation function starts the creation of Inv,Block,CmpctBlock
     * -message tasks
     * based on the used propagtion protocol
     *
     * @param neigbors list of all neigboring nodes
     * @param block    the block to propagte
     */
    abstract public void propagate(ArrayList<Node> neigbors, Block block);

    /**
     *
     * @return Flag to signal the usage of Transactions
     */
    public boolean useTransactions() {
        return USE_TRANSACTIONS;
    }

    /**
     * @param block   the block to send
     * @param subList the subgroup of neigbors to send the block to
     */
    public void sendBlock(Block block, List<Node> subList) {
        for (Node to : subList) {
            // Use RecMessageTask (without scheduling) to append a block in the messageQue
            // for sending
            msgTransMechanism.addTransmissionTask(new RecMessageTask(to, this.selfNode, block));
        }
    }

    /**
     * Let the node process the block, and propagate it if successful.
     */
    public void processBlock(Block block) {
        if (selfNode.processBlock(block)) {
            this.mempool.removeAll(block.getTransactions());
            // self mined block: processed but not received
            this.addKnownMessage(block, this.selfNode);
            HashSet<Node> informedNodes = this.knownMessages.get(block); // must have key 'block'
            Set<Node> neighbors = selfNode.getNeighbors(informedNodes);
            // check connections to these neighbors
            neighbors = selfNode.getRoutingTable().updateConnections(neighbors);
            this.propagate(new ArrayList<>(neighbors), block);
        }
    }

    public void callbackTimeout(AbstractMessage message, Node failedNode) {
    };

    // ********************************************************************************
    // Individual functions to handle the receptions of specific messages
    // ********************************************************************************

    public void handleReceivedMessage(AbstractMessageTask message) {
        if (!selfNode.isOnline()) {
            return;
        }
        // corrupted nodes ignore received block and transaction messages.
        if (selfNode.isCorrupted()) {
            return;
        }
        if (message instanceof BlockMessageTask) {
            if (this.setMsgProcessed(message)) {
                handleBlockMessage((BlockMessageTask) message);
            }
        }
        if (message instanceof TransactionTask) {
            if (this.setMsgProcessed(message)) {
                handleTransaction((TransactionTask) message);
            }
        }
    };

    public AbstractMessage getDataMsg(AbstractMessageTask task) {
        if (task instanceof BlockMessageTask) {
            return ((BlockMessageTask) task).getBlock();
        } else if (task instanceof TransactionTask) {
            return ((TransactionTask) task).getTransaction();
        } else {
            return null;
        }
    }

    /**
     * Add this message and the sender to `knownMessages` and return true if
     * the message should be further processed.
     */
    protected boolean setMsgProcessed(AbstractMessageTask message) {
        AbstractMessage msg = getDataMsg(message);
        if (msg == null) {
            return false;
        }
        boolean toProcess = !this.knownMessages.containsKey(msg);
        Node src = (message.getFrom() == null) ? selfNode : message.getFrom();
        this.addKnownMessage(msg, src);
        if (msg instanceof Block blk) {
            Simulator.updateBlkLastArrival(blk);
        }
        return toProcess;
    }

    protected void handleBlockMessage(BlockMessageTask message) {
        Block block = message.getBlock();
        debugLog(String.format("Node %s handle Block (%s) from %s", this.selfNode, block, message.getFrom()));
        block.increaseRelay(message.getFrom(), selfNode);
        this.processBlock(block);
    }

    /**
     * called if the recieved message is a transaction message
     * while the local mempool still has space and the transaction is new add the
     * transaction to the mempool and the knownMessages list
     * then send the transactions to neighbors and delay if the node is adversarial
     *
     * (the check if the transaction is known by neighbors is used to reduce the
     * number of transaction tasks in the taskqueue
     * as network congestion is not modeled a transaction message the is send and
     * refused is the same as a message that is not send)
     */
    // TODO NOTE: difference with CompactBlockRelay.handleTransaction??
    // Only delay transactions and no blocks?
    public void handleTransaction(TransactionTask message) {
        debugLog(String.format("Node %s handle Transaction from %s", this.selfNode, message.getFrom()));
        Transaction t = message.getTransaction();
        if ((this.mempool.size() < 600)) {
            this.mempool.add(t);
        }
        HashSet<Node> informedNodes = this.knownMessages.getOrDefault(t, new HashSet<Node>());
        for (Node n : this.selfNode.getNeighbors()) {
            if (!informedNodes.contains(n)) {
                informedNodes.add(n);
                TransactionTask task = new TransactionTask(this.selfNode, n, t);
                // TODO NOTE: transaction propagation also use corresponding propagation
                // protocol.
                Timer.putTask(task);
                debugLog(String.format("Node %s -> node %s: Transaction", this.selfNode, n));
            }
        }
        this.knownMessages.putIfAbsent(t, informedNodes);
    }

    public void addBlockTransmission(AbstractMessageTask msg) {
        if (!(msg instanceof IDataRequestHandler)) {
            throw new UnsupportedOperationException();
        }
        this.msgTransMechanism.addTransmissionTask((IDataRequestHandler) msg);
    }

    /**
     * clears message queue for reusing nodes in multiple simulations
     */
    public void clear() {
        this.mempool.clear();
        this.knownMessages.clear();
    }
}
