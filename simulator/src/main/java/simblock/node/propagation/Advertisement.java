package simblock.node.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

import simblock.node.Node;
import simblock.node.transmission.ParallelTransmission;
import simblock.node.transmission.SerialTransmission;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Timer;
import simblock.block.Block;
import simblock.block.AbstractMessage;
import simblock.task.*;
import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

/**
 * Propagation protocol for advertisment based propagtion
 */
public class Advertisement extends AbstractPropagationProtocol {
    /**
     * hashmap of all received invs for a block
     */
    protected HashMap<AbstractMessage, ArrayList<Node>> receivedInvs = new HashMap<>();

    /**
     * blocks the node is currently waiting for
     */
    protected final HashSet<Block> downloadingBlocks = new HashSet<>();

    public Advertisement(Node node) {
        super(node);
        msgTransMechanism = new SerialTransmission();
        // msgTransMechanism = new ParallelTransmission();
    }

    /**
     * Add the message `m` to knownMessage set as well as from whom has the node
     * heard from the message.
     * In this way the node can avoid sending messages to these nodes.
     */
    public void addReceivedInv(AbstractMessage msg, Node n) {
        ArrayList<Node> nodes = this.receivedInvs.getOrDefault(msg, new ArrayList<Node>());
        if (!nodes.contains(n)) {
            nodes.add(n);
        }
        this.receivedInvs.putIfAbsent(msg, nodes);
    }

    /**
     * Sends InvMessages to all neighbors to advertise the block
     */
    public void propagate(ArrayList<Node> neighbors, Block block) {
        Collections.shuffle(neighbors, simRng);
        this.sendInv(block, neighbors);
    }

    /**
     * Send inv.
     *
     * @param block the block
     */
    public void sendInv(Block block, List<Node> subList) {
        for (Node to : subList) {
            debugLog(String.format("Node %s -> node %s: INV (%s)", this.selfNode, to, block));
            AbstractMessageTask task = new InvMessageTask(this.selfNode, to, block);
            Timer.putTask(task);
        }
    }

    @Override
    public void handleReceivedMessage(AbstractMessageTask message) {
        super.handleReceivedMessage(message);

        if (!selfNode.isOnline()) {
            return;
        }
        if (selfNode.isCorrupted() && SimulationConfiguration.CORRUPTED_NODE_CRASH) {
            return;
        }

        if (message instanceof InvMessageTask) {
            Block block = ((InvMessageTask) message).getBlock();
            // ignore already processed the message
            if (this.knownMessages.containsKey(block)) {
                return;
            }
            // corrupted and non-responding node immediately forward INV message
            block.increaseHop(message.getFrom(), selfNode);
            selfNode.updateRumorState(block, 1);

            if (selfNode.isCorrupted()) {
                this.addKnownMessage(block, message.getFrom()); // only forward INV of each block once
                debugLog(String.format("Node %s(X) forward INV (%s) to all neighbors", selfNode, block));
                // TODO ideally can forward INV to all nodes (if dynamic connection is allowed)
                this.sendInv(block, new ArrayList<Node>(this.selfNode.getNeighbors()));
            } else {
                handleInvMessage((InvMessageTask) message);
            }
        }

        // corrupted node ignore REC (GETDATA) message
        if (selfNode.isCorrupted()) {
            return;
        }

        if (message instanceof RecMessageTask) {
            handleRecMessage((RecMessageTask) message);
        }
    }

    @Override
    protected boolean setMsgProcessed(AbstractMessageTask message) {
        if (!super.setMsgProcessed(message)) {
            return false;
        }
        AbstractMessage msg = this.getDataMsg(message); // msg not null here
        // The block/tx will be handled, the INV sender are also marked as knowing
        // the msg and thus won't be broadcast after the msg is processed.
        for (Node n : this.receivedInvs.getOrDefault(msg, new ArrayList<Node>())) {
            this.addKnownMessage(msg, n);
        }
        this.receivedInvs.remove(msg);

        return true;
    }

    @Override
    protected void handleBlockMessage(BlockMessageTask message) {
        this.downloadingBlocks.remove(message.getBlock());
        // remove all timeout task related to the block
        if (!SimulationConfiguration.CORRUPTED_NODE_CRASH) {
            for (Task task : Timer.findTasksByType("TimeoutTask")) {
                TimeoutTask timeoutTask = (TimeoutTask) task;
                if (timeoutTask.getScheduledNode() == selfNode && message.getBlock() == timeoutTask.getMessage()) {
                    debugLog(String.format("Node %s cancels scheduled timeout task for block %s", selfNode, message.getBlock()));
                    Timer.removeTask(timeoutTask);
                }
            }
        }
        super.handleBlockMessage(message);
    }

    /**
     * Called when node receives an InvMessage.
     * First check id the advertised block is needed send RecMessage if it is the
     * case
     * next set a TimeoutTask if the message sender is adversarial
     * and save following InvMessage for the repeating blocks
     *
     * @param message the InvMessage
     */
    protected void handleInvMessage(InvMessageTask message) {
        Block block = message.getBlock();
        debugLog(String.format("Node %s handle Inv (%s) from %s", this.selfNode, block, message.getFrom()));
        this.addReceivedInv(block, message.getFrom());

        if (this.selfNode.isBlockValid(block) && !this.downloadingBlocks.contains(block)) {
            this.downloadingBlocks.add(block);
            debugLog(String.format("Node %s -> node %s: Rec(GETDATA) (%s)", this.selfNode, message.getFrom(), block));
            AbstractMessageTask task = new RecMessageTask(this.selfNode, message.getFrom(), block);
            Timer.putTask(task);

            if (!SimulationConfiguration.CORRUPTED_NODE_CRASH) {
                if (message.getFrom().isChurnNode || message.getFrom().isCorrupted()) {
                    TimeoutTask t = new TimeoutTask(this.selfNode, block, message.getFrom());
                    Timer.addRemovableTask(t);
                }
            }
        }
    }

    /**
     * Called if the received message is a RecMessage.
     * If the receiving node is adversarial it calls `delayMessage` function;
     * otherwise forward it to blockSendingMechanism().
     *
     * @param message the received message
     */
    public void handleRecMessage(RecMessageTask message) {
        Block block = message.getBlock();
        debugLog(String.format("Node %s handle Rec(GETDATA) (%s) from %s", this.selfNode, block, message.getFrom()));
        block.increaseHop(selfNode, message.getFrom());
        msgTransMechanism.addTransmissionTask(message);
    }

    /**
     * Callback function for timeout tasks. It is called when a block is not
     * received after {NetworkConfiguration.TIMEOUT} ms after Inv.  The node
     * requests the block from another node that sent the same Inv.
     *
     * @param message
     */
    @Override
    public void callbackTimeout(AbstractMessage message, Node failedNode) {
        // The moment when the block (from somewhere else), this timeout task should have been removed already.
        assert !this.knownMessages.containsKey(message);

        Block block = (Block) message; // currently only deal with blocks
        debugLog(String.format("Node %s triggers timeout callback on node %s for block %s", selfNode, failedNode, block));
        ArrayList<Node> invSenders = this.receivedInvs.getOrDefault(block, new ArrayList<>());
        if (!invSenders.remove(failedNode)) {
            debugLog(String.format("[WARNING] Timeout event called on a node (%s) that did not send INV (%s) to node %s",
                    failedNode, block, this.selfNode));
            return;
        }
        if (!invSenders.isEmpty()) {
            Collections.shuffle(invSenders, simRng);    // reduce the probability that anotherNode is the same adversarial node
            Node anotherNode = invSenders.get(0);
            AbstractMessageTask task = new RecMessageTask(this.selfNode, anotherNode, block);
            Timer.putTask(task);
            // set another timeout event for the new request
            if (anotherNode.isChurnNode || anotherNode.isCorrupted()) {
                TimeoutTask t = new TimeoutTask(this.selfNode, block, anotherNode);
                Timer.addRemovableTask(t);
            }
        } else {
            this.downloadingBlocks.remove(block);
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.receivedInvs.clear();
        this.downloadingBlocks.clear();
    }
}
