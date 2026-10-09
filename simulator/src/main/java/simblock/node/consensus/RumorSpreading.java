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

package simblock.node.consensus;

import static simblock.simulator.Simulator.debugLog;

import simblock.node.Node;
import simblock.node.propagation.Push;
import simblock.node.propagation.AbstractPropagationProtocol;
import simblock.block.Rumor;
import simblock.block.RumorState;
import simblock.block.Block;
import simblock.task.CreateRumorTask;

/**
 * Class RumorSpreading does not apply consenus on messages. It only creates
 * Rumor messages randomly and nodes accept them without validation.
 */
public class RumorSpreading extends AbstractConsensusAlgo {

    /** expected number of rumors per second. */
    protected static double arrivalRate = 0;

    /**
     * Initialize the arrival rate only once
     * @param rate
     */
    public static void initArrivalRate(double rate) {
        if (arrivalRate == 0) {
            arrivalRate = rate;
        }
    }

    public static double getArrivalRate() {
        return arrivalRate;
    }

    public RumorSpreading(Node selfNode) {
        super(selfNode);
    }

    /**
     * When a rumor is created, find a random node to create the next rumor.
     */
    @Override
    public CreateRumorTask minting() {
        // The current rumor issuer schedule the next rumor issuer
        if (selfNode.getBlock().getMinter() != selfNode) {
            return null;
        }
        if (isSimulationFinished()) {
            return null;
        }
//        debugLog(String.format("Node %s schedule next rumor-creation task.", selfNode));
        // double m = - Math.log(simRng.nextDouble());
        // long interval = Math.round(m / (double) arrivalRate * 1000);    // in millisecond
        int interval = (int) Math.round(1000/arrivalRate);
        return new CreateRumorTask(selfNode.getBlock(), interval);
    }

    @Override
    public boolean isReceivedBlockValid(Block receivedBlock, Block currentBlock) {
        return true;
    }

    @Override
    public Rumor genesisBlock() {
        AbstractPropagationProtocol propagation = selfNode.getPropagationProtocol();
        if (propagation instanceof Push) {
            Rumor.endState = RumorState.EndState.ONE;
        } else {
            Rumor.endState = RumorState.EndState.TWO;
        }

        return new Rumor(selfNode, 0);
    }
}
