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

package simblock.node.routing;

import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Simulator.simRng;

import java.util.ArrayList;
import java.util.Collections;

import simblock.node.Node;
import simblock.settings.NetworkConfiguration;

/**
 * The implementation of the {@link RandomGraphRoutingTable} representing the
 * Bitcoin core routing
 * table.
 */
@SuppressWarnings("unused")
public class BitcoinRoutingTable extends RandomGraphRoutingTable {

    private boolean connectToMiners;

    /**
     * Instantiates a new Bitcoin core table.
     *
     * @param selfNode the self node
     */
    public BitcoinRoutingTable(Node selfNode, boolean connectToMiners) {
        super(selfNode);
        this.connectToMiners = connectToMiners;
    }

    public BitcoinRoutingTable(Node selfNode) {
        super(selfNode);
    }

    public void setConnectToMiners(boolean connect) {
        this.connectToMiners = connect;
    }

    @Override
    public void initTable() {
        ArrayList<Integer> candidates = new ArrayList<>();
        ArrayList<Integer> miners = new ArrayList<>();
        for (int i = 0; i < getSimulatedNodes().size(); i++) {
            if (connectToMiners && selfNode.getMiningPower() > 0
                    && getSimulatedNodes().get(i).getMiningPower() > 0) {
                miners.add(i);
            } else {
                candidates.add(i);
            }
        }
        Collections.shuffle(miners, simRng);
        Collections.shuffle(candidates, simRng);
        if (connectToMiners) {
            for (int miner : miners) {
                if (this.neighbors.size() < NetworkConfiguration.DEGREE) {
                    this.addConnectionTo(getSimulatedNodes().get(miner));
                } else {
                    break;
                }
            }
        }
        for (int candidate : candidates) {
            if (this.neighbors.size() < NetworkConfiguration.DEGREE) {
                this.addConnectionTo(getSimulatedNodes().get(candidate));
            } else {
                break;
            }
        }
    }
}
