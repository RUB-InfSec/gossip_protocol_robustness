package simblock.block;

/*
 * empty transaction class
 */
public class Transaction extends AbstractMessage {

    private static int latestTxId = 0;

    public Transaction() {
        this.id = latestTxId++;
        // TODO relay not initialized
    }
}
