package simblock.simulator;

import org.junit.jupiter.api.Test;

import simblock.block.Block;
import simblock.block.Shred;
import simblock.block.Transaction;
import simblock.node.Node;
import simblock.task.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class TimerTest {
    static String routingTableClass = "simblock.node.routing.UndirectedRoutingTable";
    static String consensusAlgoClass = "simblock.node.consensus.RumorSpreading";
    static String propagationClass = "simblock.node.propagation.Push";

    public static Node node1 = new Node(1, 0, 0,
            routingTableClass, consensusAlgoClass, propagationClass, false);
    public static Node node2 = new Node(2, 1, 0,
            routingTableClass, consensusAlgoClass, propagationClass, false);
    public static Node node3 = new Node(3, 2, 0,
            routingTableClass, consensusAlgoClass, propagationClass, false);
    public static Node node4 = new Node(4, 3, 0,
            routingTableClass, consensusAlgoClass, propagationClass, false);
    public static Block block = new Block(null, node1, 100, null);
    public static Shred shred = new Shred(2, "DATA", 1, 128, node3, block);
    public static Transaction tx = new Transaction();

    static Task blockTask = new BlockMessageTask(node1, node2, block, 0);
    static Task cmptBlockTask = new CmpctBlockMessageTask(node2, node3, block, 0);
    static Task corruptTask = new CorruptNodeTask(null, Arrays.asList(node4), 0);
    static Task rumorTask = new CreateRumorTask(block, node3, 0);
    static Task getBlockTxnTask = new GetBlockTxnMessageTask(node3, node2, block);
    static Task invTask = new InvMessageTask(node3, node2, block);
    static Task miningTask = new MiningTask(node4, 0, null);
    static Task recTask = new RecMessageTask(node2, node4, block);
    static Task shredTask = new ShredTask(node3, node4, shred);
    static Task txTask = new TransactionTask(node4, node1, tx);

    static Task spyBlockTask = spy(blockTask);
    static Task spyCmptBlockTask = spy(cmptBlockTask);
    static Task spyCorruptTask = spy(corruptTask);
    static Task spyRumorTask = spy(rumorTask);
    static Task spyGetBlockTxnTask = spy(getBlockTxnTask);
    static Task spyInvTask = spy(invTask);
    static Task spyMiningTask = spy(miningTask);
    static Task spyRecTask = spy(recTask);
    static Task spyShredTask = spy(shredTask);
    static Task spyTxTask = spy(txTask);


    public static void prepareTaskQueue() {
        Timer.reset();

        doReturn(500).when(spyBlockTask).getInterval();
        doReturn(200).when(spyCmptBlockTask).getInterval();
        doReturn(300).when(spyCorruptTask).getInterval();
        doReturn(100).when(spyRumorTask).getInterval();
        doReturn(400).when(spyGetBlockTxnTask).getInterval();
        doReturn(700).when(spyInvTask).getInterval();
        doReturn(800).when(spyMiningTask).getInterval();
        doReturn(300).when(spyRecTask).getInterval();
        doReturn(450).when(spyShredTask).getInterval();
        doReturn(350).when(spyTxTask).getInterval();

        doNothing().when(spyBlockTask).run();
        doNothing().when(spyCmptBlockTask).run();
        doNothing().when(spyCorruptTask).run();
        doNothing().when(spyRumorTask).run();
        doNothing().when(spyGetBlockTxnTask).run();
        doNothing().when(spyInvTask).run();
        doNothing().when(spyMiningTask).run();
        doNothing().when(spyRecTask).run();
        doNothing().when(spyShredTask).run();
        doNothing().when(spyTxTask).run();

        Timer.putTask(spyBlockTask);
        Timer.putTask(spyCmptBlockTask);
        Timer.putTask(spyCorruptTask);
        Timer.putTask(spyRumorTask);
        Timer.putTask(spyGetBlockTxnTask);
        Timer.putTask(spyInvTask);
        Timer.putTask(spyMiningTask);
        Timer.putTask(spyRecTask);
        Timer.putTask(spyShredTask);
        Timer.putTask(spyTxTask);
    }

    @Test
    public void testRemoveTask() {
        prepareTaskQueue();
        Task timeoutTask = new TimeoutTask(node1, block, node2);
        Task spyTimeoutTask = spy(timeoutTask);
        doReturn(500).when(spyTimeoutTask).getInterval();

        Timer.addRemovableTask(timeoutTask);
        ArrayList<Task> foundTasks = Timer.findTasksByType("TimeoutTask");
        Task foundTask = foundTasks.get(0);
        assertInstanceOf(TimeoutTask.class, foundTask);
        assertEquals(1, foundTasks.size());
        assertEquals(timeoutTask, foundTask);
        Timer.removeTask(foundTask);
        while (Timer.hasNextTask()) {
            Task task = Timer.runTask();
            assertFalse(task instanceof TimeoutTask);
        }

    }

    @Test
    public void testGetPendingMsgTasksAfterTime() {
        prepareTaskQueue();
        Map<AbstractMessageTask, Integer> tasks = Timer.getPendingMsgTasksAfterTime(0);
        assertTrue(tasks.containsKey(spyBlockTask));
        assertTrue(tasks.containsKey(spyCmptBlockTask));
        assertFalse(tasks.containsKey(spyCorruptTask));
        assertFalse(tasks.containsKey(spyRumorTask));
        assertTrue(tasks.containsKey(spyGetBlockTxnTask));
        assertTrue(tasks.containsKey(spyInvTask));
        assertFalse(tasks.containsKey(spyMiningTask));
        assertTrue(tasks.containsKey(spyRecTask));
        assertTrue(tasks.containsKey(spyShredTask));
        assertTrue(tasks.containsKey(spyTxTask));

        tasks = Timer.getPendingMsgTasksAfterTime(400);
        assertTrue(tasks.containsKey(spyBlockTask));
        assertFalse(tasks.containsKey(spyCmptBlockTask));
        assertTrue(tasks.containsKey(spyGetBlockTxnTask));
        assertTrue(tasks.containsKey(spyInvTask));
        assertFalse(tasks.containsKey(spyRecTask));
        assertTrue(tasks.containsKey(spyShredTask));
        assertFalse(tasks.containsKey(spyTxTask));
    }

}
