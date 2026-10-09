package simblock.node.propagation;

import java.util.ArrayList;

import simblock.block.Block;
import simblock.node.Node;

import static simblock.settings.SimulationConfiguration.MONERO_FLUFFY_BLOCK_USAGE;
import static simblock.simulator.Simulator.simRng;

/**
 * class that implements a propagation protocol based on the Monero blockchain
 * using direct Push
 */
public class Monero extends Cbr {

    /**
     * on creation choose whether a node uses fluffy blocks (Moneros compact blocks)
     * 
     * @param node
     */
    public Monero(Node node) {
        super(node);
        if (simRng.nextDouble() < MONERO_FLUFFY_BLOCK_USAGE) {
            this.useCBR = true;
        } else {
            this.useCBR = false;
        }
    }

    /**
     * first send fluffyblocks then normal full blocks
     */
    @Override
    public void propagate(ArrayList<Node> neighbors, Block block) {
        ArrayList<Node> neighborsWithFluffyBlocks = new ArrayList<>();
        ArrayList<Node> neighborsWithOutFluffyBlocks = new ArrayList<>();
        
        for (Node n : neighbors) {
            if (n.getPropagationProtocol().useCBR()) {
                neighborsWithFluffyBlocks.add(n);
            } else {
                neighborsWithOutFluffyBlocks.add(n);
            }
        }
        this.sendBlock(block, neighborsWithFluffyBlocks);
        this.sendBlock(block, neighborsWithOutFluffyBlocks);
    }
}
