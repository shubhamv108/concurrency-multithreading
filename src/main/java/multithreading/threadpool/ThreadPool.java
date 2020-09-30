package multithreading.threadpool;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

public class ThreadPool {

    private int corePoolSize;
    private int maximumPoolSize;
    private int ttlInMilliseconds;
    private ThreadFactory threadFactory;
    private Queue<Runnable> workQueue;
    private List<Thread> workers = new CopyOnWriteArrayList<>();
    private CommandRejectionHandler commandRejectionHandler;

    private static final int DEFAULT_MAX_WORKER_POOL_SIZE = 100;

    public ThreadPool(final int corePoolSize, final int maximumPoolSize, final int ttlInMilliseconds,
                      final ThreadFactory threadFactory, final int maxWorkQueueSize,
                      final CommandRejectionHandler commandRejectionHandler) {
        this(corePoolSize, maximumPoolSize, ttlInMilliseconds, threadFactory, commandRejectionHandler);
        this.workQueue = new LinkedBlockingQueue<>(maxWorkQueueSize);
    }

    public ThreadPool(final int corePoolSize, final int maximumPoolSize, final int ttlInMilliseconds,
                      final ThreadFactory threadFactory,  final Queue<Runnable> workQueue,
                      final CommandRejectionHandler commandRejectionHandler) {
        this(corePoolSize, maximumPoolSize, ttlInMilliseconds, threadFactory, commandRejectionHandler);
        this.workQueue = workQueue != null ? workQueue : new LinkedBlockingQueue<>(DEFAULT_MAX_WORKER_POOL_SIZE);
    }

    private ThreadPool(final int corePoolSize, final int maximumPoolSize, final int ttlInMilliseconds,
                       final ThreadFactory threadFactory, final CommandRejectionHandler commandRejectionHandler) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.ttlInMilliseconds = ttlInMilliseconds;
        this.threadFactory = threadFactory != null ? threadFactory : new CustomThreadFactory();
        this.commandRejectionHandler = commandRejectionHandler;
    }

    public <T> Future<T> submit(Callable<T> task, CommandRejectionHandler commandRejectionHandler) {
        FutureTask futureTask = new FutureTask<T>(task);
        execute(futureTask, commandRejectionHandler);
        return futureTask;
    }

    public void execute(Runnable command, CommandRejectionHandler commandRejectionHandler) {
        boolean isRejected;
        synchronized(workQueue) {
            if (workers.size() < corePoolSize || ((isRejected = !workQueue.offer(command)) && workers.size() < maximumPoolSize)) {
                Thread thread = this.threadFactory.newThread(new multithreading.threadpool.ThreadPool.Worker(command));
                this.workers.add(thread);
                thread.start();
                isRejected = false;
                workQueue.notify();
            }
        }
        if (isRejected) {
            reject(command, commandRejectionHandler);
        }
    }

    private void reject(Runnable command, CommandRejectionHandler commandRejectionHandler) {
        if (commandRejectionHandler != null) {
            commandRejectionHandler.handle(command);
        } else if (this.commandRejectionHandler != null) {
            commandRejectionHandler.handle(command);
        } else {
            // log
            System.out.println("Command Rejected: " + command.toString());
        }
    }

    private class Worker extends Thread {

        private Runnable task;

        private Worker(final Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (true) {
                if (task == null) {
                    synchronized (workQueue) {
                        while (workQueue.isEmpty()) {
                            try {
                                workQueue.wait();
                            } catch (InterruptedException e) {
                                System.out.println("An error occurred while queue is waiting: " + e.getMessage());
                            }
                        }
                        task = workQueue.poll();
                    }
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    System.out.println("Thread pool is interrupted due to an issue: " + e.getMessage());
                }
                task = null;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPool pool = new ThreadPool(2, 5, -1, null, 1, null);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("Task1");
            }

            @Override
            public String toString() {
                return "Task1";
            }
        }, null);
        pool.submit(new java.util.concurrent.Callable<Object>() {
            @Override
            public Object call() throws Exception {
                System.out.println("Task2"); return null;
            }

            @Override
            public String toString() {
                return "Task2";
            }
        }, null);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("Task3");
            }

            @Override
            public String toString() {
                return "Task3";
            }
        }, null);
        pool.submit(() -> { System.out.println("Task4"); return null; }, null);
        pool.submit(() -> { System.out.println("Task5"); return null; }, null);
        pool.submit(() -> { System.out.println("Task6"); return null; }, null);
        pool.submit(() -> { System.out.println("Task7"); return null; }, null);
    }
}

