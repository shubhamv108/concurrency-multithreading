package code.shubham.multithreading.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ReadWriteLock {

    private final Map<Thread, Integer> readingThreads = new HashMap<>();

    private Thread writingThread = null;
    private int writeRequests = 0;
    private int writeAccesses = 0;

    public synchronized void RLock() throws InterruptedException {
        Thread thread = Thread.currentThread();
        while (!(this.canGrantReadAccess(thread))) {
            wait();
        }
        this.readingThreads.put(thread, this.readingThreads.getOrDefault(thread, 0) + 1);
    }

    public synchronized void RUnlock() {
        Thread thread = Thread.currentThread();
        Integer accessCount = this.readingThreads.get(thread);
        if (accessCount != null) {
            if (accessCount == 1) this.readingThreads.remove(thread);
            else this.readingThreads.put(thread, accessCount--);
            notifyAll();
        }
    }

    public synchronized void lock() throws InterruptedException {
        this.writeRequests++;
        Thread thread = Thread.currentThread();
        while (!this.canGrantWriteAccess(thread)) {
            wait();
        }
        this.writeRequests--;
        this.writeAccesses++;
        this.writingThread = thread;
    }

    public synchronized void unlock() {
        if (this.isWriter(Thread.currentThread())) {
            this.writeAccesses--;
            if(this.writeAccesses == 0){
                this.writingThread = null;
            }
            notifyAll();
        }
    }

    public boolean canGrantWriteAccess(Thread thread) {
        if (this.isOnlyReader(thread)) return true;
        if (this.hasReaders()) return false;
        if (this.writingThread == null) return true;
        if (!this.isWriter(thread)) return true;
        return true;
    }

    public boolean canGrantReadAccess(Thread thread) {
        if (this.isWriter(thread)) return true;
        if (this.hasWriter()) return false;
        if (this.isReader(thread)) return true;
        if (this.hasWriteRequests()) return false;
        return true;
    }

    public boolean isWriter(Thread thread) {
        return Objects.equals(thread, this.writingThread);
    }

    public boolean isReader(Thread thread) {
        return this.readingThreads.get(thread) != null;
    }

    public boolean isOnlyReader(Thread thread) {
        return this.readingThreads.get(thread) != null && this.readingThreads.size() == 1;
    }

    public boolean hasReaders() {
        return this.readingThreads.size() > 0;
    }

    public boolean hasWriter() {
        return this.writingThread != null;
    }

    public boolean hasWriteRequests() {
        return this.writeRequests > 0;
    }

}
