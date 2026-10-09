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

import static simblock.simulator.Simulator.simRng;

import simblock.simulator.Simulator;
import simblock.simulator.Timer;
import simblock.node.Node;
import simblock.node.propagation.AbstractPropagationProtocol;
import simblock.block.Rumor;
import simblock.block.Block;

import java.util.List;

/**
 * A task to create a new rumor
 */
public class CreateRumorTask extends AbstractMintingTask {

    public CreateRumorTask(Block parent, Node initiator, int interval) {
        super(initiator, interval);
        this.parent = parent;
    }


    public CreateRumorTask(Block parent, int interval) {
        this(parent, null, interval);
    }

    @Override
    public void run() {
        // select the next honest node to create a new rumor now, as in adaptive settings, nodes
        // can be honest when schedule the task but become corrupted when task is due.
        List<Node> nodes = Simulator.getSimulatedNodes();
        do {
            minter = nodes.get(simRng.nextInt(nodes.size()));
        } while (minter.isCorrupted() || !minter.isOnline());
        AbstractPropagationProtocol propagation = minter.getPropagationProtocol();
        Rumor rumor = new Rumor(minter, Timer.getCurrentTime());
//        debugLog(String.format("Node %s created new rumor (ID=%s).", minter, rumor));
        propagation.processBlock(rumor);
    }
}
