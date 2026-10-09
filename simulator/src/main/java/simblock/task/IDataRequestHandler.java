package simblock.task;

public interface IDataRequestHandler {

    /**
     * Handle the data request from the message task.
     *
     * @param bwSharedBy add restrictions to shared bandwidth if multiple messages are sent in parallel.
     * @return a new send data task to be scheduled
     */
    public AbstractMessageTask handleDataRequest(int bwSharedBy);
}
