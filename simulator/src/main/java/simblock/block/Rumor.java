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

package simblock.block;

import java.util.HashSet;

import simblock.node.Node;
import simblock.settings.SimulationConfiguration;

/*
 * Rumor message that no need for block validation.
 * Only distinctive (sequential) ID is required.
 */
public class Rumor extends Block {

    /** Use global emtpyTxSet when initialize a rumor as it extends Block */
    private static final HashSet<Transaction> EMPTY_TX_SET = new HashSet<>();

    /**
     * Note that the parent is null, the height is set to be the same as ID.
     */
    public Rumor(Node originator, int createdTime) {
        super(null, originator, createdTime, EMPTY_TX_SET);
    }

    @Override
    protected void initRelay(Node issuer) {
        super.initRelay(issuer);
        issuer.updateRumorState(this, endState.getValue());
    }

    /** 
     * increase the relay when the full message arrives the `dest` node and also
     * update the rumor state on the `dest` node.
     */
    @Override
    public boolean increaseRelay(Node src, Node dest) {
        if (super.increaseRelay(src, dest)) {
            dest.updateRumorState(this, endState.getValue());
            return true;
        }
        return false;
    }

    @Override
    public boolean increaseHop(Node src, Node dest) {
        if (!dest.rumorReachEndState(this)) {
            return super.increaseHop(src, dest);
        }
        return false;
    }

    /**
     * Rumor does not append to another, set to its monotonically incremented ID.
     */
    @Override
    public int getHeight() {
        return this.id;
    }

    @Override
    public Block getBlockWithHeight(int height) {
        System.out.println("[WARNING] getBlockWithHeight() should NOT be called on Rumor message.");
        return null;
    }

    @Override
    public boolean isOnSameChainAs(Block block) {
        return true;
    }

    @Override
    public String toString() {
        return String.format("%d", getId());
    }
}
