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

import java.util.HashSet;
import java.util.Set;

/**
 * Advertise a batch of blocks to nodes
 */
public class InvBatchMessageTask extends AbstractMessageTask {

    /**
     * Block to be advertised.
     */
    private final Set<Block> blocks;

    /**
     * Instantiates a new Inv message task.
     *
     * @param from  the sender
     * @param to    the receiver
     * @param blocks the blocks to be advertised
     */
    public InvBatchMessageTask(Node from, Node to, Set<Block> blocks) {
        super(from, to);
        this.blocks = new HashSet<>(blocks);
    }

    /**
     * Gets block.
     *
     * @return the block
     */
    public Set<Block> getBlocks() {
        return this.blocks;
    }

    @Override
    public AbstractMessage getMessage() {
        return null;
    }

}