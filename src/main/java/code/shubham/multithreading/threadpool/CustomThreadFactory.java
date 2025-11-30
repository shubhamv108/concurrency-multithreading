package multithreading.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {

    private ThreadGroup threadGroup;
    private String threadNamePrefix;

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private static final int DEFAULT_STACK_SIZE = 0;

    public CustomThreadFactory() {
//        this.threadGroup = System.getSecurityManager() != null ?
//                System.getSecurityManager().getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.threadNamePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.threadGroup, runnable, threadNamePrefix + threadNumber.getAndIncrement(),
                DEFAULT_STACK_SIZE);

        if (thread.isDaemon())
            thread.setDaemon(false);
        if (thread.getPriority() != Thread.NORM_PRIORITY)
            thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
