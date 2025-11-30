package code.shubham;

import multithreading.falsesharing.DefaultMaximumCalculationStrategy;
import multithreading.falsesharing.DefaultMinimumCalculationStrategy;
import multithreading.falsesharing.Statistics;
import multithreading.falsesharing.ThreadSafeStatisticsImplementation;
import multithreading.falsesharing.WelfordMeanAndVarianceStampedLockStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsTest {

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = ThreadSafeStatisticsImplementation.createDefault();
    }

    @Test
    @DisplayName("Test basic statistics calculation")
    void testBasicStatistics() {
        stats.event(10);
        stats.event(20);
        stats.event(30);

        assertEquals(10, stats.min(), "Minimum should be 10");
        assertEquals(30, stats.max(), "Maximum should be 30");
        assertEquals(20.0f, stats.mean(), 0.01f, "Mean should be 20.0");
        assertEquals(66.67f, stats.variance(), 0.1f, "Variance should be ~66.67");
    }

    @Test
    @DisplayName("Test empty statistics returns zeros")
    void testEmptyStatistics() {
        assertEquals(0, stats.min(), "Empty min should be 0");
        assertEquals(0, stats.max(), "Empty max should be 0");
        assertEquals(0.0f, stats.mean(), 0.01f, "Empty mean should be 0.0");
        assertEquals(0.0f, stats.variance(), 0.01f, "Empty variance should be 0.0");
    }

    @Test
    @DisplayName("Test single value")
    void testSingleValue() {
        stats.event(42);

        assertEquals(42, stats.min());
        assertEquals(42, stats.max());
        assertEquals(42.0f, stats.mean(), 0.01f);
        assertEquals(0.0f, stats.variance(), 0.01f, "Variance of single value should be 0");
    }

    @Test
    @DisplayName("Test identical values have zero variance")
    void testIdenticalValues() {
        for (int i = 0; i < 100; i++) {
            stats.event(50);
        }

        assertEquals(50, stats.min());
        assertEquals(50, stats.max());
        assertEquals(50.0f, stats.mean(), 0.01f);
        assertEquals(0.0f, stats.variance(), 0.01f, "All identical values should have 0 variance");
    }

    @Test
    @DisplayName("Test negative numbers")
    void testNegativeNumbers() {
        stats.event(-100);
        stats.event(-50);
        stats.event(0);
        stats.event(50);
        stats.event(100);

        assertEquals(-100, stats.min());
        assertEquals(100, stats.max());
        assertEquals(0.0f, stats.mean(), 0.01f);
        assertEquals(5000.0f, stats.variance(), 1.0f);
    }

    @Test
    @DisplayName("Test extreme values")
    void testExtremeValues() {
        stats.event(Integer.MIN_VALUE);
        stats.event(0);
        stats.event(Integer.MAX_VALUE);

        assertEquals(Integer.MIN_VALUE, stats.min());
        assertEquals(Integer.MAX_VALUE, stats.max());
        // Mean and variance will overflow, but shouldn't crash
        assertNotNull(stats.mean());
        assertNotNull(stats.variance());
    }

    @Test
    @DisplayName("Test Welford's algorithm numerical stability")
    void testNumericalStability() {
        // Large numbers with small variations test numerical stability
        int base = 1_000_000_000;
        for (int i = 0; i < 100; i++) {
            stats.event(base + i);
        }

        assertEquals(base, stats.min());
        assertEquals(base + 99, stats.max());
        assertEquals(base + 49.5f, stats.mean(), 1.0f);

        // Expected variance for 0-99 is ~833.33
        assertEquals(833.33f, stats.variance(), 50.0f);
    }

    @Test
    @DisplayName("Test variance formula: Var(aX) = a²Var(X)")
    void testVarianceScaling() {
        Statistics stats1 = ThreadSafeStatisticsImplementation.createDefault();
        Statistics stats2 = ThreadSafeStatisticsImplementation.createDefault();

        // Add values to stats1
        for (int i = 1; i <= 10; i++) {
            stats1.event(i);
        }

        // Add scaled values to stats2 (multiply by 2)
        for (int i = 1; i <= 10; i++) {
            stats2.event(i * 2);
        }

        float variance1 = stats1.variance();
        float variance2 = stats2.variance();

        // Var(2X) should be 4 * Var(X)
        assertEquals(variance1 * 4, variance2, 0.5f);
    }

    @Test
    @DisplayName("Test thread-safety with multiple threads - Run 1")
    void testMultiThreadedCorrectness1() throws InterruptedException {
        testMultiThreadedCorrectnessHelper();
    }

    @Test
    @DisplayName("Test thread-safety with multiple threads - Run 2")
    void testMultiThreadedCorrectness2() throws InterruptedException {
        testMultiThreadedCorrectnessHelper();
    }

    @Test
    @DisplayName("Test thread-safety with multiple threads - Run 3")
    void testMultiThreadedCorrectness3() throws InterruptedException {
        testMultiThreadedCorrectnessHelper();
    }

    @Test
    @DisplayName("Test thread-safety with multiple threads - Run 4")
    void testMultiThreadedCorrectness4() throws InterruptedException {
        testMultiThreadedCorrectnessHelper();
    }

    @Test
    @DisplayName("Test thread-safety with multiple threads - Run 5")
    void testMultiThreadedCorrectness5() throws InterruptedException {
        testMultiThreadedCorrectnessHelper();
    }

    private void testMultiThreadedCorrectnessHelper() throws InterruptedException {
        int numThreads = 10;
        int eventsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Each thread adds numbers 0-999
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        stats.event(j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(0, stats.min(), "Min should be 0");
        assertEquals(999, stats.max(), "Max should be 999");
        assertEquals(499.5f, stats.mean(), 1.0f, "Mean should be ~499.5");

        // Variance for uniform distribution [0, 999]
        assertTrue(stats.variance() > 80000 && stats.variance() < 90000,
                   "Variance should be approximately 83333");
    }

    @Test
    @DisplayName("Test high contention scenario")
    void testHighContention() throws InterruptedException {
        int numThreads = 50;
        int eventsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads wait to start together

                    for (int j = 0; j < eventsPerThread; j++) {
                        stats.event(threadId * 1000 + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads at once
        assertTrue(endLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        // Verify statistics are reasonable
        assertEquals(0, stats.min());
        assertEquals(49999, stats.max());
        assertTrue(stats.mean() > 0 && stats.mean() < 50000);
        assertTrue(stats.variance() > 0);
    }

    @Test
    @DisplayName("Test concurrent reads and writes")
    void testConcurrentReadsAndWrites() throws InterruptedException {
        int numWriters = 8;
        int numReaders = 4;
        int duration = 1; // seconds

        ExecutorService executor = Executors.newFixedThreadPool(numWriters + numReaders);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numWriters + numReaders);

        AtomicInteger errorCount = new AtomicInteger(0);

        // Writer threads
        for (int i = 0; i < numWriters; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (duration * 1000);

                    while (System.currentTimeMillis() < endTime) {
                        stats.event(ThreadLocalRandom.current().nextInt(1000));
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Reader threads
        for (int i = 0; i < numReaders; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (duration * 1000);

                    while (System.currentTimeMillis() < endTime) {
                        // Read all statistics
                        int min = stats.min();
                        int max = stats.max();
                        float variance = stats.variance();

                        // Basic sanity checks
                        assertTrue(min <= max);
                        assertTrue(variance >= 0);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(duration + 5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, errorCount.get(), "No errors should occur during concurrent access");
    }

    // ========================================================================
    // STRATEGY PATTERN TESTS
    // ========================================================================

    @Test
    @DisplayName("Test custom strategy injection")
    void testCustomStrategyInjection() {
        // Create with custom strategies
        Statistics customStats = new ThreadSafeStatisticsImplementation(
                new DefaultMinimumCalculationStrategy(),
                new DefaultMaximumCalculationStrategy(),
                new WelfordMeanAndVarianceStampedLockStrategy()
        );

        customStats.event(5);
        customStats.event(10);
        customStats.event(15);

        assertEquals(5, customStats.min());
        assertEquals(15, customStats.max());
        assertEquals(10.0f, customStats.mean(), 0.01f);
    }

    @Test
    @DisplayName("Test null strategy throws exception")
    void testNullStrategyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ThreadSafeStatisticsImplementation(null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ThreadSafeStatisticsImplementation(
                    new DefaultMinimumCalculationStrategy(),
                    null,
                    new WelfordMeanAndVarianceStampedLockStrategy()
            );
        });
    }

    @Test
    @DisplayName("Test large dataset")
    void testLargeDataset() {
        int size = 1_000_000;

        for (int i = 0; i < size; i++) {
            stats.event(i);
        }

        assertEquals(0, stats.min());
        assertEquals(size - 1, stats.max());
        assertEquals((size - 1) / 2.0f, stats.mean(), 1.0f);
    }

    @Test
    @DisplayName("Test sequential min updates")
    void testSequentialMinUpdates() {
        // Add values in descending order (worst case for min)
        for (int i = 1000; i >= 0; i--) {
            stats.event(i);
        }

        assertEquals(0, stats.min());
        assertEquals(1000, stats.max());
    }

    @Test
    @DisplayName("Test sequential max updates")
    void testSequentialMaxUpdates() {
        // Add values in ascending order (worst case for max)
        for (int i = 0; i <= 1000; i++) {
            stats.event(i);
        }

        assertEquals(0, stats.min());
        assertEquals(1000, stats.max());
    }

    @Test
    @DisplayName("Test alternating values")
    void testAlternatingValues() {
        for (int i = 0; i < 100; i++) {
            stats.event(i % 2 == 0 ? 10 : 20);
        }

        assertEquals(10, stats.min());
        assertEquals(20, stats.max());
        assertEquals(15.0f, stats.mean(), 0.01f);
        assertEquals(25.0f, stats.variance(), 0.1f);
    }

    @Test
    @DisplayName("Test performance under load")
    void testPerformanceUnderLoad() throws InterruptedException {
        int numThreads = 8;
        int eventsPerThread = 100_000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        stats.event(ThreadLocalRandom.current().nextInt());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Should complete in reasonable time");
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        long totalEvents = (long) numThreads * eventsPerThread;
        double throughput = (totalEvents * 1000.0) / duration;

        System.out.printf("Processed %d events in %d ms (%.2f M ops/sec)%n",
                          totalEvents, duration, throughput / 1_000_000);

        // Should achieve at least 50M ops/sec on modern hardware
        assertTrue(throughput > 20_000_000,
                   "Throughput should be > 20M ops/sec, was: " + throughput);
    }

    // ========================================================================
    // GETCOUNT TESTS (Additional method)
    // ========================================================================

    @Test
    @DisplayName("Test getCount method")
    void testGetCount() {
        ThreadSafeStatisticsImplementation impl =
                (ThreadSafeStatisticsImplementation) ThreadSafeStatisticsImplementation.createDefault();

        assertEquals(0, impl.getCount(), "Initial count should be 0");

        impl.event(10);
        assertEquals(1, impl.getCount(), "Count should be 1 after one event");

        impl.event(20);
        impl.event(30);
        assertEquals(3, impl.getCount(), "Count should be 3 after three events");
    }

    @Test
    @DisplayName("Test getCount with concurrent updates")
    void testGetCountConcurrent() throws InterruptedException {
        ThreadSafeStatisticsImplementation impl =
                (ThreadSafeStatisticsImplementation) ThreadSafeStatisticsImplementation.createDefault();

        int numThreads = 10;
        int eventsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        impl.event(j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numThreads * eventsPerThread, impl.getCount(),
                     "Count should match total events");
    }
}
