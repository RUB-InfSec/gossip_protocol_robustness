package simblock.node.propagation;

import static simblock.simulator.Simulator.simRng;

import java.util.ArrayList;
import java.util.Collections;
import simblock.block.Block;
import simblock.node.Node;
import simblock.node.transmission.ParallelTransmission;
import simblock.node.transmission.SerialTransmission;

/**
 * propagation protocol that directly forwards every block to all neighbors
 */
public class Push extends AbstractPropagationProtocol {
    public Push(Node node) {
        super(node);
        msgTransMechanism = new SerialTransmission();
    //    msgTransMechanism = new ParallelTransmission();
    }

    @Override
    public void propagate(ArrayList<Node> neighbors, Block block) {
        Collections.shuffle(neighbors, simRng);
        this.sendBlock(block, neighbors);
    }
}
