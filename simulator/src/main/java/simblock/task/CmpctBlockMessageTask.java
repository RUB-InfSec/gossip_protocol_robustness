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

/**
 * The type Compact block message task.
 */
// Compact block relay protocol Wiki:
// https://github.com/bitcoin/bips/blob/master/bip-0152.mediawiki
public class CmpctBlockMessageTask extends AbstractMessageTask {
    /**
     * The {@link Block} that is sent.
     */
    private final Block block;

    /**
     * The block message sending delay in milliseconds.
     */
    private final int interval;

    /**
     * The transmission-only portion of {@link #interval} (processing time +
     * serialization time), excluding the one-way propagation delay.
     */
    private final int transmissionDelay;

    /**
     * Same as {@link #transmissionDelay} but at microsecond precision.
     */
    private final long transmissionDelayMicros;

    /**
     * Instantiates a new Compact block message task.
     *
     * @param from  the sender
     * @param to    the receiver
     * @param block the block instance
     * @param delay the delay of the message transmission
     */
    public CmpctBlockMessageTask(Node from, Node to, Block block, int delay) {
        this(from, to, block, delay, delay * 1000L);
    }

    /**
     * Instantiates a new Compact block message task with a microsecond-precise
     * transmission delay, for callers that need to preserve sub-millisecond
     * transmission time (see {@link AbstractMessageTask#getTransmissionDelayMicros()}).
     *
     * @param from                    the sender
     * @param to                      the receiver
     * @param block                   the block instance
     * @param delay                   the delay of the message transmission, in milliseconds
     * @param transmissionDelayMicros the transmission-only portion of {@code delay}, in microseconds
     */
    public CmpctBlockMessageTask(Node from, Node to, Block block, int delay, long transmissionDelayMicros) {
        super(from, to);
        this.block = block;
        this.transmissionDelay = delay;
        this.transmissionDelayMicros = transmissionDelayMicros;
        this.interval = super.getInterval() + delay;
    }

    @Override
    public int getInterval() {
        return this.interval;
    }

    @Override
    public int getTransmissionDelay() {
        return this.transmissionDelay;
    }

    @Override
    public long getTransmissionDelayMicros() {
        return this.transmissionDelayMicros;
    }

    /**
     * Sends a new compact block message from the sender to the receiver and logs
     * the event.
     */
    @Override
    public void run() {
        super.run();
    }

    /**
     * Get block.
     *
     * @return the block
     */
    public Block getBlock() {
        return this.block;
    }

    @Override
    public AbstractMessage getMessage() {
        return this.block;
    }
}
