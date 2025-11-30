package multithreading.locks;

import java.util.concurrent.atomic.AtomicReference;

public class SpinLock {

    private final AtomicReference<Thread> threadAtomicReference = new AtomicReference<>();

    public void lock() {
        while (!this.threadAtomicReference.compareAndSet(null, Thread.currentThread())) {}
    }
    
    public void unlock() {
        this.threadAtomicReference.compareAndSet(Thread.currentThread(), null);
    }

}