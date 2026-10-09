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

import simblock.node.transmission.IMsgTransmission;

/**
 * Frees a sender's transmission slot once a message has been fully pushed
 * onto the wire (after the transmission delay only), independent of when the
 * message actually arrives at the receiver. Arrival additionally incurs a
 * one-way propagation delay that does not occupy the sender's link, so this
 * task is scheduled separately from (and fires no later than) the
 * corresponding message's delivery.
 */
public class EndTransmissionTask implements Task {

    private final IMsgTransmission transmission;
    private final int interval;

    /**
     * Instantiates a new End transmission task.
     *
     * @param transmission the transmission mechanism whose slot is freed
     * @param interval     the transmission-only delay, in milliseconds
     */
    public EndTransmissionTask(IMsgTransmission transmission, int interval) {
        this.transmission = transmission;
        this.interval = interval;
    }

    @Override
    public int getInterval() {
        return this.interval;
    }

    @Override
    public void run() {
        this.transmission.endMessageTransmission();
    }
}
