/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.task;

import simblock.block.AbstractMessage;
import simblock.block.Block;
import simblock.node.Node;
import simblock.node.propagation.Cbr;
import simblock.node.propagation.Monero;
import simblock.simulator.Network;

import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Network.computeTransmissionDelay;
import static simblock.simulator.Network.computeTransmissionDelayMicros;
import static simblock.simulator.Simulator.debugLog;;

/**
 * The type Rec message task to request to retrieve a block.
 * When it is put in the Timer task, it is equivalent to GETDATA message. When
 * it is in the messageQueue, it just indicate the next message to be sent.
 */

public class RecMessageTask extends AbstractMessageTask implements IDataRequestHandler {

    /**
     * The block to be received.
     */
    private final Block block;

    /**
     * Instantiates a new Rec message task.
     *
     * @param from  the sending node
     * @param to    the receiving node
     * @param block the block to be received
     */
    public RecMessageTask(Node from, Node to, Block block) {
        super(from, to);
        this.block = block;
    }

    /**
     * Gets the block to be received.
     *
     * @return the block
     */
    public Block getBlock() {
        return this.block;
    }

    protected boolean isCmptMode(Node from, Node to) {
        if (from.getPropagationProtocol() instanceof Cbr) {
            return from.getPropagationProtocol().useCBR() && to.getPropagationProtocol().useCBR();
        }
        if (from.getPropagationProtocol() instanceof Monero) {
            return to.getPropagationProtocol().useCBR();
        }
        return false;
    }

    @Override
    public AbstractMessageTask handleDataRequest(int bwSharedBy) {
        // send data to the requested node
        Node from = getTo();
        Node to = getFrom();

        if (from.getPropagationProtocol().knowsMessage(to, block)) {
//            debugLog(String.format("%s -> %s (blk %s) canceled, %s already knows the message", from, to, block, to));
            return null;
        }

        int processingTime = from.getPropagationProtocol().getProcessingTime();
        int delay = processingTime;
        long processingTimeMicros = processingTime * 1000L;
        long bandwidth = Network.getBandwidth(from.getRegion(), to.getRegion());
        AbstractMessageTask sendDataTask;
        // send compact block if accepted by both nodes (sending and recieving)
        if (this.isCmptMode(from, to)) {
            long transmissionDelayMicros = processingTimeMicros
                    + computeTransmissionDelayMicros(COMPACT_BLOCK_SIZE, bandwidth / bwSharedBy);
            delay += computeTransmissionDelay(COMPACT_BLOCK_SIZE, bandwidth / bwSharedBy);
            sendDataTask = new CmpctBlockMessageTask(from, to, block, delay, transmissionDelayMicros);
            debugLog(String.format("Node %s -> node %s: CmpctBlock (%s)", from, to, block));
        } else {
            long transmissionDelayMicros = processingTimeMicros
                    + computeTransmissionDelayMicros(BLOCK_SIZE, bandwidth / bwSharedBy);
            delay += computeTransmissionDelay(BLOCK_SIZE, bandwidth / bwSharedBy);
            sendDataTask = new BlockMessageTask(from, to, block, delay, transmissionDelayMicros);
            debugLog(String.format("Node %s -> node %s: Block (%s)", from, to, block));
        }
        return sendDataTask;
    }

    @Override
    public AbstractMessage getMessage() {
        return this.block;
    }

}
