package simblock.node.propagation;

import simblock.block.AbstractMessage;
import simblock.block.Block;
import simblock.node.Node;
import simblock.node.routing.GossipsubRoutingTable;
import simblock.settings.SimulationConfiguration;
import simblock.simulator.Timer;
import simblock.task.*;

import java.util.*;

import static simblock.simulator.Simulator.debugLog;
import static simblock.simulator.Simulator.simRng;

public class Gossipsub extends Hybrid {

    // record the messages seen since the last delivery of gossip batch
    protected HashSet<Block> latestMessages = new HashSet<>();

    // record the validation time (reception time) of each rumor message
    protected HashMap<AbstractMessage, Integer> msgValidateTimeRecords = new HashMap<>();

    public Gossipsub(Node node) {
        super(node);

        if (!(node.getRoutingTable() instanceof GossipsubRoutingTable)) {
            throw new RuntimeException("GossipSub propagation protocol requires gossipsub routing table class");
        }
    }

    /**
     * If rumor message is duplicated, update the mesh message deliveries counter.
     */
    @Override
    protected boolean setMsgProcessed(AbstractMessageTask message) {
        if (!super.setMsgProcessed(message)) {
            if (selfNode != ((Block) message.getMessage()).getMinter()) {
                ((GossipsubRoutingTable) selfNode.getRoutingTable()).markDupMsgDelivery(message.getFrom(), msgValidateTimeRecords.get(message.getMessage()));
            }
            return false;
        }
        return true;
    }

    public void garbageCollect(int removeInfoUptoId) {
//        msgValidateTimeRecords.keySet().removeIf(msg -> msg.getId() <= removeInfoUptoId);
        receivedInvs.keySet().removeIf(msg -> msg.getId() <= removeInfoUptoId);
        downloadingBlocks.removeIf(msg -> msg.getId() <= removeInfoUptoId);
        knownMessages.forEach((k, v) -> {
            if (k.getId() <= removeInfoUptoId) {
                v.clear();
            }
        });
    }

    @Override
    public void handleReceivedMessage(AbstractMessageTask message) {
        // reduce the memory
        if (message instanceof BlockMessageTask blkmsgTask) {
            if (blkmsgTask.getBlock().getId() > 0 && blkmsgTask.getBlock().getId() % 1000 == 0) {
                garbageCollect(blkmsgTask.getBlock().getId()-1000);
            }
        }

        if (!selfNode.isOnline()) {
            return;
        }
        if (selfNode.isCorrupted() && SimulationConfiguration.CORRUPTED_NODE_CRASH) {
            // selectively drop message (1 out of 10) and handle the rest
//            double droprate = SimulationConfiguration.SIM_CONFIGS.adversary.msgDropRate;
//            if (droprate < 1 && droprate > 0) {
//                if (message instanceof BlockMessageTask blkmsgTask) {
//                    Block block = blkmsgTask.getBlock();
//                    Random rng = new Random(block.getId()); // all corrupted nodes synchronized to drop the same messages using the same rng seed
//                    if (rng.nextDouble() <= droprate) {
                        // drop the message
            int dropInterval = (int) SimulationConfiguration.SIM_CONFIGS.adversary.msgDropRate;
            if (dropInterval > 1) {
                if (message instanceof BlockMessageTask blkmsgTask) {
                    Block block = blkmsgTask.getBlock();
                    if (block.getId() % dropInterval == 0) {
                        // drop the message
                    } else {
                        if (super.setMsgProcessed(message)) {
                            debugLog(String.format("Corrupted Node %s handle Block (%s) from %s", this.selfNode, block, message.getFrom()));
                            block.increaseRelay(message.getFrom(), selfNode);
                            this.processBlock(block);
                        }
                    }
                }
            }
            return;
        }

        super.handleReceivedMessage(message);

        // no INV messages, only INVBatch messages
        assert !(message instanceof InvMessageTask);

        // InvBatchMessageTask is equivalent to IHAVE message in gossipsub
        // RecMessageTask then serves as IWANT message for each msg request
        if (message instanceof InvBatchMessageTask) {
            for (Block block: ((InvBatchMessageTask) message).getBlocks()) {
                // skip already processed the message
                if (this.knownMessages.containsKey(block)) {
                    continue;
                }
                // pull each unseen message as an INV message
                handleInvMessage(new InvMessageTask(message.getFrom(), message.getTo(), block));
            }
        }
    }

    /**
     * Directly push a processed block to mesh connections (neighbors),
     * and periodically advertise batch of messages.
     * @param neighbors list of all neighboring nodes
     * @param block    the block to propagate
     */
    @Override
    public void propagate(ArrayList<Node> neighbors, Block block) {
        latestMessages.add(block);

        Collections.shuffle(neighbors, simRng);
        this.sendBlock(block, neighbors);

    }

    /**
     * Update counter of the first message delivery
     */
    @Override
    protected void handleBlockMessage(BlockMessageTask message) {
        ((GossipsubRoutingTable) selfNode.getRoutingTable()).markFirstMsgDelivery(message.getFrom());
        msgValidateTimeRecords.put(message.getMessage(), Timer.getCurrentTime());

        super.handleBlockMessage(message);
    }

    // Lazy pull advertise message to random nodes
    public void sendBatchGossip(List<Node> nodes) {
        // corrupted nodes do not send batch inv, rather, it immediately advertise INV for any received INV or block message.
        if (selfNode.isCorrupted()) {
            return;
        }
        if (!latestMessages.isEmpty()) {
            for (Node to : nodes) {
                debugLog(String.format("Node %s -> node %s: INV Batch", this.selfNode, to));
                AbstractMessageTask task = new InvBatchMessageTask(this.selfNode, to, latestMessages);
                Timer.putTask(task);
            }
            latestMessages.clear();
        }
    }
}
