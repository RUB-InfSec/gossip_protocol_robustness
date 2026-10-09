package simblock.node.propagation;

import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

import java.util.ArrayList;
import java.util.Collections;
import simblock.block.Block;
import simblock.node.Node;
import simblock.settings.SimulationConfiguration;
import simblock.task.AbstractMessageTask;
import simblock.task.BlockMessageTask;
import simblock.task.InvMessageTask;

/**
 * class to implement ethereum style square-root propagtion
 */
public class Hybrid extends Advertisement {

    public Hybrid(Node node) {
        super(node);
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

        // When a non-responding node receives a block message through PUSH, he
        // immediately sends INV of the pushed message to all neighbors
        if (selfNode.isCorrupted() && (message instanceof BlockMessageTask)) {
            Block block = ((BlockMessageTask) message).getBlock();
            // already processed the message
            if (this.knownMessages.containsKey(block)) {
                return;
            }
            // only increase hop but not the relay so that honest nodes received INV can also increase hop
            block.increaseHop(message.getFrom(), selfNode);
            selfNode.updateRumorState(block, 1);
            this.addKnownMessage(block, message.getFrom()); // only forward INV of each block once
            // TODO ideally can forward INV to all nodes (if dynamic connection is allowed)
            debugLog(String.format("Node %s(X) forward INV (%s) to all neighbors", selfNode, block));
            this.sendInv(block, new ArrayList<>(this.selfNode.getNeighbors()));
        }
    }

    /**
     * first calculate border = squareroot of current number of neighbors
     * than directly send the block to border neigbors and send Inv message to all
     * others
     */
    @Override
    public void propagate(ArrayList<Node> neighbors, Block block) {
        int border = this.calculatePropagation(neighbors);
        Collections.shuffle(neighbors, simRng);
        this.sendBlock(block, neighbors.subList(0, border));
        this.sendInv(block, neighbors.subList(border, neighbors.size()));
    }

    public int calculatePropagation(ArrayList<Node> neigbors) {
        return Math.round(Math.round(Math.sqrt(neigbors.size())));
    }
}
