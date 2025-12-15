package code.shubham.multithreading.locks;

import java.util.concurrent.locks.StampedLock;

/**
 * READ, WRITE and OPTIMISTIC READ locks
 */
public class StampedLockDemo {

    int count = 0;
    private final StampedLock lock = new StampedLock();

    public void put() {
        long stamp = lock.writeLock();
        try {
            ++count;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public int get() throws InterruptedException {
        long stamp = lock.readLock();
        try {
            return count;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public int readWithOptimisticLock() {
        long stamp = lock.tryOptimisticRead();
        if(!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                return count;
            } finally {
                lock.unlock(stamp);
            }
        }
        return count;
    }

}
