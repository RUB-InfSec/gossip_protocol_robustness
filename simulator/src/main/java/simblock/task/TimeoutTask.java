package simblock.task;

import simblock.node.Node;
import simblock.block.AbstractMessage;
import simblock.block.Block;
import static simblock.settings.NetworkConfiguration.TIMTOUT;

/**
 * Task for NetworkTimeouts when waiting to long for blocks
 */
public class TimeoutTask implements Task {

    // Node where the task occurred
    private Node node;
    // Neighbor of `node` that failed and caused the timeout event.
    private Node failedNode;

    private AbstractMessage msg;

    public TimeoutTask(Node node, AbstractMessage message, Node failedNode) {
        this.msg = message;
        this.node = node;
        this.failedNode = failedNode;
    }

    /**
     * calls back to waiting node
     */
    @Override
    public void run() {
        this.node.getPropagationProtocol().callbackTimeout(msg, failedNode);
    }

    @Override
    public int getInterval() {
        return TIMTOUT;
    }

    public AbstractMessage getMessage() {
        return msg;
    }

    public Node getScheduledNode() {
        return node;
    }

    public Node getFailedNode() {
        return failedNode;
    }
}
