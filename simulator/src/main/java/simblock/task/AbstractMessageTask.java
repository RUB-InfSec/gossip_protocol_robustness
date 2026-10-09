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

import static simblock.simulator.Network.getEdgeLatency;

import simblock.block.AbstractMessage;
import simblock.node.Node;

/**
 * The type Abstract message task.
 */
public abstract class AbstractMessageTask implements Task {
    /**
     * The sending entity.
     */
    private final Node from;
    /**
     * The receiving entity.
     */
    private final Node to;

    /**
     * Instantiates a new Abstract message task.
     *
     * @param from the sending entity
     * @param to   the receiving entity
     */
    public AbstractMessageTask(Node from, Node to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Get the sending node.
     *
     * @return the <em>from</em> node
     */
    public Node getFrom() {
        return this.from;
    }

    /**
     * Get the receiving node.
     *
     * @return the <em>to</em> node
     */
    public Node getTo() {
        return this.to;
    }

    /**
     * Get the link delay with regards to respective regions.
     *
     * @return the message sending interval
     */
    public int getInterval() {
        int latency = getEdgeLatency(this.from.getRegion(), this.to.getRegion());
        return latency;
    }

    /**
     * Receive message at the <em>to</em> side.
     */
    public void run() {
        this.to.receiveMessage(this);
    }

    /**
     * Time the sender's link is occupied transmitting this message (processing
     * time + serialization time at the available bandwidth), excluding the
     * one-way propagation delay to the receiver. Used to free up the sender's
     * transmission slot as soon as the message has been fully pushed onto the
     * wire, instead of waiting for it to also arrive at the receiver.
     *
     * @return the transmission-only delay, or 0 for tasks that don't occupy a
     *         shared transmission slot
     */
    public int getTransmissionDelay() {
        return 0;
    }

    /**
     * Same as {@link #getTransmissionDelay()} but at microsecond precision. The
     * simulation clock itself stays millisecond-granular, so this is used only by
     * callers that carry a sub-millisecond remainder across several messages
     * (see {@link simblock.node.transmission.SerialTransmission}) rather than
     * truncating each message's transmission time to 0 individually.
     *
     * @return the transmission-only delay in microseconds
     */
    public long getTransmissionDelayMicros() {
        return this.getTransmissionDelay() * 1000L;
    }

    public abstract AbstractMessage getMessage();

}
