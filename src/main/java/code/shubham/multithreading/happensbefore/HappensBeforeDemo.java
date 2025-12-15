package code.shubham.multithreading.happensbefore;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates Happens-Before relationships in Java Memory Model.
 *
 * Happens-Before guarantees that operations in one thread are visible to another thread.
 * Without happens-before, the JVM can reorder operations, causing visibility issues.
 */

// ============================================================================
// EXAMPLE 1: NO HAPPENS-BEFORE (BROKEN CODE)
// ============================================================================
class BrokenVisibility {
    private int data = 0;
    private boolean ready = false;

    // Writer thread
    public void writer() {
        data = 42;           // Write 1
        ready = true;        // Write 2
        // NO happens-before guarantee!
        // Compiler/CPU can reorder: ready might be set before data
    }

    // Reader thread
    public int reader() {
        while (!ready) {     // Read 1
            Thread.yield();
        }
        return data;         // Read 2 - might still see 0!
        // NO guarantee data write is visible even though ready is true
    }
}

// ============================================================================
// EXAMPLE 2: VOLATILE - Creates Happens-Before
// ============================================================================
class VolatileHappensBefore {
    private int data = 0;
    private volatile boolean ready = false;  // volatile keyword

    public void writer() {
        data = 42;           // Write 1
        ready = true;        // Write 2 (volatile write)
        // HAPPENS-BEFORE: All writes before volatile write are visible
        // after the volatile read
    }

    public int reader() {
        while (!ready) {     // Read (volatile read)
            Thread.yield();
        }
        return data;         // GUARANTEED to see 42!
        // Happens-before ensures data write is visible
    }
}

// ============================================================================
// EXAMPLE 3: SYNCHRONIZED - Creates Happens-Before
// ============================================================================
class SynchronizedHappensBefore {
    private int data = 0;
    private boolean ready = false;
    private final Object lock = new Object();

    public void writer() {
        synchronized (lock) {    // Lock acquisition
            data = 42;
            ready = true;
        }                        // Lock release
        // HAPPENS-BEFORE: All writes in synchronized block are visible
        // to next thread that acquires the same lock
    }

    public int reader() {
        synchronized (lock) {    // Lock acquisition
            while (!ready) {
                try {
                    lock.wait(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return data;         // GUARANTEED to see 42!
        }
    }
}

// ============================================================================
// EXAMPLE 4: REENTRANTLOCK - Creates Happens-Before
// ============================================================================
class LockHappensBefore {
    private int data = 0;
    private boolean ready = false;
    private final Lock lock = new ReentrantLock();

    public void writer() {
        lock.lock();
        try {
            data = 42;
            ready = true;
        } finally {
            lock.unlock();       // Unlock
        }
        // HAPPENS-BEFORE: unlock() → lock() guarantees visibility
    }

    public int reader() {
        lock.lock();             // Lock acquisition sees previous unlock
        try {
            while (!ready) {
                lock.unlock();
                Thread.sleep(10);
                lock.lock();
            }
            return data;         // GUARANTEED to see 42!
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            lock.unlock();
        }
    }
}

// ============================================================================
// EXAMPLE 5: THREAD START - Creates Happens-Before
// ============================================================================
class ThreadStartHappensBefore {
    private int data = 0;

    public void example() {
        data = 42;               // Write in parent thread

        Thread t = new Thread(() -> {
            System.out.println(data);  // GUARANTEED to see 42!
        });

        t.start();               // Thread.start() creates happens-before
        // All writes before start() are visible in the new thread
    }
}

// ============================================================================
// EXAMPLE 6: THREAD JOIN - Creates Happens-Before
// ============================================================================
class ThreadJoinHappensBefore {
    private int result = 0;

    public void example() throws InterruptedException {
        Thread t = new Thread(() -> {
            result = 42;         // Write in child thread
        });

        t.start();
        t.join();                // Wait for thread to finish

        System.out.println(result);  // GUARANTEED to see 42!
        // Thread.join() creates happens-before: all writes in thread
        // are visible after join() returns
    }
}

// ============================================================================
// EXAMPLE 7: FINAL FIELDS - Creates Happens-Before
// ============================================================================
class FinalFieldHappensBefore {
    private final int data;      // final field
    private final String name;

    public FinalFieldHappensBefore(int data, String name) {
        this.data = data;
        this.name = name;
        // Constructor completion happens-before any thread sees this object
    }

    // Any thread that gets a reference to this object is GUARANTEED
    // to see the correct values of final fields (no null/0)
}

// ============================================================================
// EXAMPLE 8: REAL-WORLD SCENARIO - Producer/Consumer
// ============================================================================
class ProducerConsumer {
    private String message = null;
    private volatile boolean hasMessage = false;  // Visibility flag

    // Producer thread
    public void produce(String msg) {
        message = msg;           // Write data
        hasMessage = true;       // Volatile write - creates happens-before
        // All previous writes (including message) are now visible
        // to any thread that reads hasMessage
    }

    // Consumer thread
    public String consume() {
        while (!hasMessage) {    // Volatile read
            Thread.yield();
        }
        hasMessage = false;      // Reset
        return message;          // GUARANTEED to see the message
        // Happens-before chain ensures visibility
    }
}

// ============================================================================
// DEMONSTRATION
// ============================================================================
public class HappensBeforeDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Testing Volatile Happens-Before ===");
        testVolatile();

        System.out.println("\n=== Testing Synchronized Happens-Before ===");
        testSynchronized();

        System.out.println("\n=== Testing Thread Start Happens-Before ===");
        testThreadStart();

        System.out.println("\n=== Testing Thread Join Happens-Before ===");
        testThreadJoin();
    }

    private static void testVolatile() throws InterruptedException {
        VolatileHappensBefore example = new VolatileHappensBefore();

        Thread writer = new Thread(example::writer);
        Thread reader = new Thread(() -> {
            int result = example.reader();
            System.out.println("Reader saw: " + result);
        });

        reader.start();
        Thread.sleep(10);
        writer.start();

        writer.join();
        reader.join();
    }

    private static void testSynchronized() throws InterruptedException {
        SynchronizedHappensBefore example = new SynchronizedHappensBefore();

        Thread writer = new Thread(example::writer);
        Thread reader = new Thread(() -> {
            int result = example.reader();
            System.out.println("Reader saw: " + result);
        });

        reader.start();
        Thread.sleep(10);
        writer.start();

        writer.join();
        reader.join();
    }

    private static void testThreadStart() {
        ThreadStartHappensBefore example = new ThreadStartHappensBefore();
        example.example();
    }

    private static void testThreadJoin() throws InterruptedException {
        ThreadJoinHappensBefore example = new ThreadJoinHappensBefore();
        example.example();
    }
}

/**
 * KEY HAPPENS-BEFORE RULES:
 *
 * 1. Program Order: Each action in a thread happens-before every subsequent action
 * 2. Monitor Lock: Unlock happens-before every subsequent lock on same monitor
 * 3. Volatile: Write to volatile happens-before every subsequent read of that volatile
 * 4. Thread Start: Thread.start() happens-before any action in started thread
 * 5. Thread Join: All actions in thread happen-before Thread.join() returns
 * 6. Transitivity: If A happens-before B, and B happens-before C, then A happens-before C
 * 7. Final Fields: Constructor writes to final fields happen-before any thread sees object
 *
 * WITHOUT HAPPENS-BEFORE:
 * - Compiler can reorder instructions
 * - CPU can reorder operations
 * - Values can be cached in CPU registers
 * - No visibility guarantees between threads
 *
 * WITH HAPPENS-BEFORE:
 * - Operations are visible in the specified order
 * - Memory barrier ensures cache coherence
 * - Guarantees thread-safe communication
 */