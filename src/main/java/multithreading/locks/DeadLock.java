package multithreading.locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DeadLock {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void operation() {
        try {
            lock.readLock().lock();
            lock.writeLock().lock(); // deadlock
        } finally {
            lock.writeLock().unlock();
            lock.readLock().unlock();
        }
    }

    public static void main(String[] args) {
        new DeadLock().operation();
    }

}
