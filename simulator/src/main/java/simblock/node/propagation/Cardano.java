package simblock.node.propagation;

import java.util.ArrayList;

import simblock.block.Block;
import simblock.node.Node;
import simblock.node.transmission.ParallelTransmission;

/**
 * class to implement a propagation protocol based on the ouroboros network of
 * the cardano blockchain based on advertisement-based Push.
 * https://ouroboros-network.cardano.intersectmbo.org/pdfs/network-spec/network-spec.pdf
 */
public class Cardano extends Advertisement {

    public Cardano(Node node) {
        super(node);
        useCBR = false;
        msgTransMechanism = new ParallelTransmission();
    }

    public void propagate(ArrayList<Node> neighbors, Block block) {
        this.sendInv(block, neighbors);
    }
}
