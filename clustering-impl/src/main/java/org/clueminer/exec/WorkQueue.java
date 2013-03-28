package org.clueminer.exec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A utility class that receives a collection of tasks to execute internally and
 * then distributes the tasks among a thread pool. This class offers to methods
 * of use. In the first, a user can pass in a collection of tasks to run and
 * then wait until the tasks are finished.
 * <pre>
 *Collection<Runnable> tasks = new LinkedList<Runnable>();
 *WorkQueue q = new WorkQueue();
 *for (int i = 0; i < numTasks; ++i)
 *    tasks.add(new Runnable() { }); // job to do goes here
 *q.run(tasks);
 *</pre> <br>
 *
 * Alternately, a use may register a task group identifier and then iteratively
 * add new tasks associated with that identifier. At some point in the future,
 * the user can then wait for all the tasks associated with that identifier to
 * finish. This second method allows for the iterative construction of tasks, or
 * for cases where not all of the data for the tasks is availabe at once
 * (although the number of tasks is known).
 * <pre>
 *WorkQueue q = new WorkQueue();
 *Object taskGroupId = Thread.currentThread(); // a unique id
 *q.registerTaskGroup(taskGroupId, numTasks);
 *for (int i = 0; i < numTasks; ++i)
 *    q.add(taskGroupId, new Runnable() { }); // job to do goes here
 *q.await(taskGroupId);
 *</pre>
 *
 * In the above example, the current thread is used as the group identifier,
 * which ensures that any other thread executing the same code won't use the
 * same identifier, which could result in either thread returning prematurely
 * before its tasks have finished. However, a <i>shared</i> group identifier can
 * allow multiple threads to add tasks for a common goal, with each being able
 * await until all the tasks are finished.
 *
 * @author David Jurgens
 */
public class WorkQueue {

    /**
     * The list of all threads drawing work from the queue.
     */
    private final List<Thread> threads;
    /**
     * The queue from which worker threads draw jobs to execute
     */
    private final BlockingQueue<Runnable> workQueue;
    /**
     * A mapping from a group identifier to the associated latch.
     */
    private final ConcurrentMap<Object, CountDownLatch> taskKeyToLatch;
    /**
     * The singleton {@code WorkQueue} instance supported by this class
     */
    private static WorkQueue singleton;

    /**
     * Creates a new work queue with the number of threads executing tasks the
     * same as the number as processors on the system.
     */
    WorkQueue() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a new work queue with the specified number of threads executing
     * tasks.
     */
    WorkQueue(int numThreads) {
        workQueue = new LinkedBlockingQueue<Runnable>();
        threads = new ArrayList<Thread>();
        taskKeyToLatch = new ConcurrentHashMap<Object, CountDownLatch>();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new WorkerThread(workQueue);
            threads.add(t);
            t.start();
        }
    }

    /**
     * Adds the provided task to the work queue on behalf of the task group
     * identifier. Note that unlike the {@link #run(Collection) run} method,
     * this method returns immediately without waiting for the task to finish.
     *
     * @param taskGroupId an identifier associated with a set of tasks.
     * @param task a task to run
     *
     * @throws IllegalArgumentException if the {@code taskGroupId} is not
     * currently associated with any active taskGroup
     */
    public void add(Object taskGroupId, Runnable task) {
        CountDownLatch latch = taskKeyToLatch.get(taskGroupId);
        if (latch == null) {
            throw new IllegalArgumentException("Unknown task id: " + taskGroupId);
        }
        if (task == null) {
            throw new NullPointerException("Cannot add null tasks");
        }
        workQueue.offer(new CountingRunnable(task, latch));
    }

    /**
     * Increases the number of concurrently processing threads by one.
     */
    private void addThread() {
        Thread t = new WorkerThread(workQueue);
        threads.add(t);
        t.start();
    }

    /**
     * Waits until all the tasks associated with the group identifier have
     * finished. Once a task group has been successfully waited upon, the group
     * identifier is removed from the queue and is valid to be reused for a new
     * task group.
     *
     * @throws IllegalArgumentException if the {@code taskGroupId} is not
     * currently associated with any active taskGroup
     */
    public void await(Object taskGroupId) {
        CountDownLatch latch = taskKeyToLatch.get(taskGroupId);
        if (latch == null) {
            throw new IllegalArgumentException("Unknown task group: " + taskGroupId);
        }
        try {
            while (!latch.await(5, TimeUnit.SECONDS)) {
                ;
            }

            // Once finished, remove the key so it can be associated with a new
            // task
            taskKeyToLatch.remove(taskGroupId);
        } catch (InterruptedException ie) {
            throw new IllegalStateException("Not all tasks finished", ie);
        }
    }

    /**
     * Waits until all the tasks associated with the group identifier have
     * finished. Once a task group has been successfully waited upon, the group
     * identifier is removed from the queue and is valid to be reused for a new
     * task group.
     *
     * @throws IllegalArgumentException if the {@code taskGroupId} is not
     * currently associated with any active taskGroup
     */
    public boolean await(Object taskGroupId, long timeout, TimeUnit unit) {
        CountDownLatch latch = taskKeyToLatch.get(taskGroupId);
        if (latch == null) {
            throw new IllegalArgumentException("Unknown task group: " + taskGroupId);
        }
        try {
            if (latch.await(timeout, unit)) {
                // Once finished, remove the key so it can be associated with a
                // new task
                taskKeyToLatch.remove(taskGroupId);
                return true;
            }
            return false;
        } catch (InterruptedException ie) {
            throw new IllegalStateException("Not all tasks finished", ie);
        }
    }

    /**
     * Returns the number of tasks that need to be completed before the group
     * associated with the key is complete. Note that this number includes both
     * those tasks running and not yet completed, as well as tasks that have yet
     * to be enqueued on behalf of this id.
     *
     * @param taskGroupId the key associated with a task group
     *
     * @return the number of tasks remaining or 0 if no group is associated with
     * that task key
     */
    public long getRemainingTasks(Object taskGroupId) {
        CountDownLatch latch = taskKeyToLatch.get(taskGroupId);
        return (latch == null)
                ? 0
                : latch.getCount();
    }

    /**
     * Returns the canonical instance of the {@link WorkQueue} to be used in
     * running concurrent tasks.
     */
    public static WorkQueue getWorkQueue() {
        synchronized (WorkQueue.class) {
            if (singleton == null) {
                // REMINDER: default number of threads?
                singleton = new WorkQueue();
            }
        }
        return singleton;
    }

    /**
     * Returns the canonical instance of the {@link WorkQueue} to be used in
     * running concurrent tasks, ensuring the <i>at least</i> the specified
     * number of threads are available.
     */
    public static WorkQueue getWorkQueue(int numThreads) {
        synchronized (WorkQueue.class) {
            if (singleton == null) {
                singleton = new WorkQueue(numThreads);
            }
        }

        while (singleton.availableThreads() < numThreads) {
            singleton.addThread();
        }
        return singleton;
    }

    /**
     * Registers a new task group with the specified number of tasks to execute
     * and returns a task group identifier to use when registering its tasks.
     *
     * @param numTasks the number of tasks that will be eventually run as a part
     * of this group.
     *
     * @returns an identifier associated with a group of tasks
     */
    public Object registerTaskGroup(int numTasks) {
        Object key = new Object();
        taskKeyToLatch.putIfAbsent(key, new CountDownLatch(numTasks));
        return key;
    }

    /**
     * Registers a new task group with the specified number of tasks to execute,
     * or returns {@code false} if a task group with the same identifier has
     * already been registered. This identifier will remain valid in the queue
     * until {@link #await(Object) await} has been called.
     *
     * @param taskGroupId an identifier to be associated with a group of tasks
     * @param numTasks the number of tasks that will be eventually run as a part
     * of this group.
     *
     * @returns {@code true} if a new task group was registered or {@code false}
     * if a task group with the same identifier had already been registered.
     */
    public boolean registerTaskGroup(Object taskGroupId, int numTasks) {
        return taskKeyToLatch.
                putIfAbsent(taskGroupId, new CountDownLatch(numTasks)) == null;
    }

    /**
     * Executes the tasks using a thread pool and returns once all tasks have
     * finished.
     *
     * @throws IllegalStateException if interrupted while waiting for the tasks
     * to finish
     */
    public void run(Runnable... tasks) {
        run(Arrays.asList(tasks));
    }

    /**
     * Executes the tasks using a thread pool and returns once all tasks have
     * finished.
     *
     * @throws IllegalStateException if interrupted while waiting for the tasks
     * to finish
     */
    public void run(Collection<Runnable> tasks) {
        // Create a semphore that the wrapped runnables will execute
        int numTasks = tasks.size();
        CountDownLatch latch = new CountDownLatch(numTasks);
        for (Runnable r : tasks) {
            if (r == null) {
                throw new NullPointerException("Cannot run null tasks");
            }
            workQueue.offer(new CountingRunnable(r, latch));
        }
        try {
            // Wait until all the tasks have finished
            latch.await();
        } catch (InterruptedException ie) {
            throw new IllegalStateException("Not all tasks finished", ie);
        }
    }

    /**
     * Returns the number of threads that are available to this {@code
     * WorkQueue} for processing the enqueued tasks.
     */
    public int availableThreads() {
        return threads.size();
    }

    /**
     * A utility class that wraps an existing runnable and updates the latch
     * when the task has finished.
     */
    private static class CountingRunnable implements Runnable {

        /**
         * The task to execute
         */
        private final Runnable task;
        /**
         * The latch to update once the task has finished
         */
        private final CountDownLatch latch;

        public CountingRunnable(Runnable task, CountDownLatch latch) {
            this.task = task;
            this.latch = latch;
        }

        /**
         * Executes the task and count down once finished.
         */
        @Override
        public void run() {
            try {
                task.run();
            } finally {
                latch.countDown();
            }
        }
    }
}
