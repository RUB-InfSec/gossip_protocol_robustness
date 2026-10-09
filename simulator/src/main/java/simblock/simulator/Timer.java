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

package simblock.simulator;

import static simblock.settings.SimulationConfiguration.FILTER_MINING_TASKS;
import static simblock.settings.SimulationConfiguration.MAX_MINING_TASKS;

import java.util.*;
import java.util.stream.Collectors;

import simblock.task.AbstractMessageTask;
import simblock.task.Task;
import simblock.block.Block;

/**
 * The type Timer schedules the execution of simulation tasks stored in a Future
 * Event List (FEL)
 * . Each {@link Task}
 * can be scheduled for execution. Tasks that have been run get removed from the
 * FEL.
 */
public class Timer {

    /**
     * A sorted queue of scheduled tasks.
     */
    private static final PriorityQueue<ScheduledTask> taskQueue = new PriorityQueue<>();

    private static final HashSet<Task> removableTasks = new HashSet<>();
    // Instead of removing task from the queue which takes a lot of time, save
    // the removed tasks so that when a task is in the removedTasks, simply
    // ignore it.
    private static final HashSet<Task> removedTasks = new HashSet<>();

    /**
     * Initial simulation time in milliseconds.
     */
    private static int currentTime = 0; // increased every time runTask()
    public static int TASK_COUNTS = 0;

    public static final Map<Block, Integer[]> BlockMiningTaksMap = new HashMap<>();

    /**
     * Represents a {@link Task} that is scheduled to be executed.
     */
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        private final Task task;
        private final int scheduledTime;

        /**
         * Instantiates a new ScheduledTask.
         *
         * @param task          - the task to be executed
         * @param scheduledTime - the simulation time at which the task is to be
         *                      executed
         */
        private ScheduledTask(Task task, int scheduledTime) {
            this.task = task;
            this.scheduledTime = scheduledTime;
        }

        /**
         * Gets the task.
         *
         * @return the {@link Task} instance
         */
        private Task getTask() {
            return this.task;
        }

        /**
         * Gets the scheduled time at which the task is to be executed.
         *
         * @return the scheduled time
         */
        private int getScheduledTime() {
            return this.scheduledTime;
        }

        /**
         * Compares the two scheduled tasks.
         *
         * @param o other task
         * @return 1 if self is executed later, 0 if concurrent and -1 if self is to be
         *         executed before.
         */
        @Override
        public int compareTo(ScheduledTask o) {
            if (this.equals(o)) {
                return 0;
            }
            int order = Integer.signum(this.scheduledTime - o.scheduledTime);
            if (order != 0) {
                return order;
            }
            order = System.identityHashCode(this) - System.identityHashCode(o);
            return order;
        }
    }

    public static void reset() {
        taskQueue.clear();
        removableTasks.clear();
        removedTasks.clear();
        currentTime = 0;
        TASK_COUNTS = 0;
    }

    /**
     * Runs a {@link ScheduledTask}.
     */
    public static Task runTask() {
        // If there are any tasks
        if (taskQueue.size() > 0) {
            if (TASK_COUNTS > 0 && TASK_COUNTS % 100000 == 0) {
                System.gc();
            }
            // Get the next ScheduledTask
            ScheduledTask currentScheduledTask = taskQueue.poll();
            Task currentTask = currentScheduledTask.getTask();
            removableTasks.remove(currentTask);
            if (removedTasks.contains(currentTask)) {
                // skip the removed task
                removedTasks.remove(currentTask);
                return null;
            }
            currentTime = currentScheduledTask.getScheduledTime();
            // Remove the task from the mapping of all tasks
            // taskMap.remove(currentTask, currentScheduledTask);
            // Execute
            currentTask.run();
            Simulator.updateSentMessageStats(currentTask);
            return currentTask;
        }
        return null;
    }

    /**
     * Remove task from the mapping of all tasks and from the execution queue.
     *
     * @param task the task to be removed
     */
    public static void removeTask(Task task) {
        removedTasks.add(task);
    }

    /**
     * Get the {@link Task} from the execution queue to be executed next.
     *
     * @return the task from the queue or null if task queue is empty.
     */
    public static boolean hasNextTask() {
        return taskQueue.size() > 0;
    }

    /**
     * Schedule task to be executed at the current time incremented by the task
     * duration.
     * The task is not cancellable. To add a cancellable task, use interface
     * `addRemovableTask`.
     *
     * @param task the task
     */
    public static ScheduledTask putTask(Task task) {
        ScheduledTask scheduledTask = new ScheduledTask(task, currentTime + task.getInterval());
        taskQueue.add(scheduledTask);
        TASK_COUNTS++;
        return scheduledTask;
    }

    /**
     * add mining task to queue and hashmap to keep them removable
     *
     * @param task mining task
     */
    public static void addRemovableTask(Task task) {
        putTask(task);
        removableTasks.add(task);
    }

    /**
     * In the removeable tasks, find tasks with a specific type
     *
     * @param taskType the type of task to search
     * @return a list of matched tasks, empty list if not found
     */
    public static ArrayList<Task> findTasksByType(String taskType) {
        String className = "simblock.task." + taskType;
        ArrayList<Task> found = new ArrayList<>();
        try {
            Class<?> clazz = Class.forName(className);
            for (Task task : removableTasks) {
                if (clazz.isInstance(task)) {
                    found.add(task);
                }
            }
            return found;
        } catch (ClassNotFoundException e) {
            System.out.printf("Task type '%s' not found.\n", className);
            return new ArrayList<>();
        }
    }

    /**
     * Get current time in milliseconds.
     *
     * @return the time
     */
    public static int getCurrentTime() {
        return currentTime;
    }

    /**
     * Return a list of message tasks that are scheduled after a certain time.
     * @param delay after the current time
     * @return the tasks as well as the remaining time before execution after the node is corrupted
     */
    public static HashMap<AbstractMessageTask, Integer> getPendingMsgTasksAfterTime(int delay) {
        List<ScheduledTask> tasks = taskQueue.stream()
                .filter(task -> task.getScheduledTime() >= currentTime + delay && (task.getTask() instanceof AbstractMessageTask))
                .collect(Collectors.toList());
        HashMap<AbstractMessageTask, Integer> pendingTasks = new HashMap<>();
        for (ScheduledTask task : tasks) {
            pendingTasks.put((AbstractMessageTask) task.getTask(), task.getScheduledTime() - currentTime - delay);
        }
        return pendingTasks;
    }

    /**
     * if the mining taks are filtered to reduce the size of the task queue
     * this function calcultes whether an new mining taskis added
     *
     * @param block    the block
     * @param interval time until mining
     * @return if the mining task is added
     */
    public static boolean checkMiningTask(Block block, int interval) {
        if (!FILTER_MINING_TASKS) {
            return true;
        }
        if (BlockMiningTaksMap.containsKey(block)) {
            if (BlockMiningTaksMap.get(block)[0] < MAX_MINING_TASKS || BlockMiningTaksMap.get(block)[1] < interval) {
                BlockMiningTaksMap.replace(block, new Integer[] { BlockMiningTaksMap.get(block)[0] + 1,
                        Math.min(BlockMiningTaksMap.get(block)[1], interval) });
                return true;
            } else {
                return false;
            }
        } else {
            BlockMiningTaksMap.put(block, new Integer[] { 0, getCurrentTime() + interval });
            return true;
        }
    }
}
