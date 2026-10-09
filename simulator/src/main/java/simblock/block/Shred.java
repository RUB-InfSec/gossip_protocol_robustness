package simblock.block;

import simblock.node.Node;

public class Shred extends AbstractMessage {
    /** Type of the shred whether it is data or code */
    public String type;
    /** Slot of the block generated */
    public int slot;
    /** Size of the shred */
    public int size;
    /** Leader of the slot (who generated the block) */
    public Node leader;
    /** corresponding block */
    public Block block;

    public Shred(int id, String type, int slot, int size, Node leader, Block block) {
        this.id = id;
        this.type = type;
        this.slot = slot;
        this.block = block;
        // if (type.equals("DATA")) {
        //     this.size = size + 88 + 83;
        // } else if (type.equals("CODE")) {
        //     this.size = +89 + 83;
        // }
        this.size = size;
        this.leader = leader;

        initRelay(leader);
    }

    @Override
    public String getMessageName() {
        return String.format("Shred-%d-%d", slot, id);
    }
}
