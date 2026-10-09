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
import simblock.simulator.Network;

import static simblock.simulator.Simulator.simRng;
import static simblock.settings.SimulationConfiguration.*;
import static simblock.simulator.Simulator.debugLog;

/**
 * The type GetBlockTxn message task.
 */
// Compact block relay protocol Wiki:
// https://github.com/bitcoin/bips/blob/master/bip-0152.mediawiki
public class GetBlockTxnMessageTask extends AbstractMessageTask implements IDataRequestHandler {
    /**
     * The {@link Block} that is sent by from as compact block.
     */
    private Block block;

    public GetBlockTxnMessageTask(Node from, Node to, Block block) {
        super(from, to);
        this.block = block;
    }

    /**
     * Get block.
     *
     * @return the block
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * Gets block size when the node fails compact block relay.
     */
    private int getFailedBlockSize() {
        if (getTo().isChurnNode) {
            int index = simRng.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE.length);
            return (int) (BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE[index]);
        } else {
            int index = simRng.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE.length);
            return (int) (BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE[index]);
        }
    }

    @Override
    public AbstractMessageTask handleDataRequest(int bwSharedBy) {
        // send data to the requested node
        Node from = getTo();
        Node to = getFrom();
        debugLog(String.format("Node %s -> node %s: Block (failedBlock) (%s)", from, to, block));

        int processingTime = from.getPropagationProtocol().getProcessingTime();
        long bandwidth = Network.getBandwidth(from.getRegion(), to.getRegion());
        long sharedBandwidth = bandwidth / bwSharedBy;
        int failedBlockSize = getFailedBlockSize();
        int delay = processingTime + Network.computeTransmissionDelay(failedBlockSize, sharedBandwidth);
        long transmissionDelayMicros = processingTime * 1000L
                + Network.computeTransmissionDelayMicros(failedBlockSize, sharedBandwidth);
        return new BlockMessageTask(from, to, block, delay, transmissionDelayMicros);
    }

    @Override
    public AbstractMessage getMessage() {
        return this.block;
    }
}
