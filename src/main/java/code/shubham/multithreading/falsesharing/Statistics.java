package code.shubham.multithreading.falsesharing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

public interface Statistics {
    /**
     * The only method which takes events as input.
     * @param n
     */
    void event(int id, int n);
    /**
     * Minimum of all the events consumed
     * @return
     */
    int min();
    /**
     * Maximum of all the events consumed
     * @return
     */
    int max();
    /**
     * Mean of all the events consumed
     * @return
     */
    float mean();
    /**

     * Variance of all the events consumed
     * @return
     */
    float variance();
}

interface MinimumCalculationStrategy {
    void update(int n);
    int getMinimum();
}

interface MaximumCalculationStrategy {
    void update(int value);
    int getMaximum();
}

interface MeanAndVarianceCalculationStrategy {
    void update(int value);
    float getMean();
    float getVariance();
    long getCount();
}

class ThreadSafeStatisticsImplementation implements Statistics {

    private final MinimumCalculationStrategy minimumCalculationStrategy;
    private final MaximumCalculationStrategy maximumCalculationStrategy;
    private final MeanAndVarianceCalculationStrategy meanAndVarianceCalculationStrategy;

    public ThreadSafeStatisticsImplementation(
            final MinimumCalculationStrategy minimumCalculationStrategy,
            final MaximumCalculationStrategy maximumCalculationStrategy,
            final MeanAndVarianceCalculationStrategy meanAndVarianceCalculationStrategy) {

        if (minimumCalculationStrategy == null) {
            throw new IllegalArgumentException("MinimumCalculationStrategy cannot be null");
        }
        if (maximumCalculationStrategy == null) {
            throw new IllegalArgumentException("MaximumCalculationStrategy cannot be null");
        }
        if (meanAndVarianceCalculationStrategy == null) {
            throw new IllegalArgumentException("MeanAndVarianceCalculationStrategy cannot be null");
        }

        this.minimumCalculationStrategy = minimumCalculationStrategy;
        this.maximumCalculationStrategy = maximumCalculationStrategy;
        this.meanAndVarianceCalculationStrategy = meanAndVarianceCalculationStrategy;
    }

    public static Statistics createDefault() {
        return new ThreadSafeStatisticsImplementation(
            new DefaultMinimumCalculationStrategy(),
            new DefaultMaximumCalculationStrategy(),
            new WelfordMeanAndVarianceStampedLockStrategy()
        );
    }

    @Override
    public void event(int id, int n) {
        minimumCalculationStrategy.update(n);
        maximumCalculationStrategy.update(n);
        meanAndVarianceCalculationStrategy.update(n);
    }

    @Override
    public int min() {
        return minimumCalculationStrategy.getMinimum();
    }

    @Override
    public int max() {
        return maximumCalculationStrategy.getMaximum();
    }

    @Override
    public float mean() {
        return meanAndVarianceCalculationStrategy.getMean();
    }

    @Override
    public float variance() {
        return meanAndVarianceCalculationStrategy.getVariance();
    }

    long getCount() {
        return meanAndVarianceCalculationStrategy.getCount();
    }
}

class DefaultMinimumCalculationStrategy implements MinimumCalculationStrategy {

    private final AtomicInteger minimum = new AtomicInteger(Integer.MAX_VALUE);

    @Override
    public void update(int value) {
        while (true) {
            int current = minimum.get();
            if (value >= current)
                return;
            if (minimum.compareAndSet(current, value))
                return;
        }
    }

    @Override
    public int getMinimum() {
        int value = minimum.get();
        return value == Integer.MAX_VALUE ? 0 : value;
    }
}

class DefaultMaximumCalculationStrategy implements MaximumCalculationStrategy {

    private final AtomicInteger maximum = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public void update(int value) {
        while (true) {
            int current = maximum.get();
            if (value <= current)
                return;
            if (maximum.compareAndSet(current, value))
                return;
        }
    }

    @Override
    public int getMaximum() {
        int value = maximum.get();
        return value == Integer.MIN_VALUE ? 0 : value;
    }
}

class WelfordMeanAndVarianceStampedLockStrategy implements MeanAndVarianceCalculationStrategy {

    private final StampedLock lock = new StampedLock();

    @jdk.internal.vm.annotation.Contended
    private long count = 0;

    @jdk.internal.vm.annotation.Contended
    private double mean = 0.0;

    @jdk.internal.vm.annotation.Contended
    private double m2 = 0.0;

    @Override
    public void update(int value) {
        long stamp = lock.writeLock();
        try {
            ++count;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private double getDoubleValue(StampedLock lock, Reader<Double> reader) {
        long stamp = lock.tryOptimisticRead();
        double result = reader.read();

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = reader.read();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    private interface Reader<T> {
        T read();
    }

    @Override
    public float getMean() {
        return (float) getDoubleValue(lock, () -> this.mean);
    }

    @Override
    public float getVariance() {
        long currentCount;
        double currentM2;

        long stamp = lock.tryOptimisticRead();
        currentCount = this.count;
        currentM2 = this.m2;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentCount = this.count;
                currentM2 = this.m2;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        if (currentCount <= 1)
            return 0.0f;

        return (float) (currentM2 / currentCount);
    }

    @Override
    public long getCount() {
        long stamp = lock.tryOptimisticRead();
        long currentCount = this.count;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentCount = this.count;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return currentCount;
    }
}

class WelfordMeanAndVarianceThreadLocalAggregationStrategy implements MeanAndVarianceCalculationStrategy {

    // Thread-local buffers
    private final ThreadLocal<LocalStats> threadLocalStats = ThreadLocal.withInitial(LocalStats::new);

    // Track all thread-local instances for flushing
    private final ConcurrentHashMap<Thread, LocalStats> allThreadLocals = new ConcurrentHashMap<>();

    // Global aggregated stats (protected by lock)
    private final StampedLock lock = new StampedLock();
    private long globalCount = 0;
    private double globalMean = 0.0;
    private double globalM2 = 0.0;

    private static class LocalStats {
        long count = 0;
        double mean = 0.0;
        double m2 = 0.0;
        int batchSize = 0;
        static final int BATCH_THRESHOLD = 1000;  // Merge after 1000 events

        void addValue(int value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
            ++batchSize;
        }

        boolean shouldFlush() {
            return batchSize >= BATCH_THRESHOLD;
        }

        void reset() {
            count = 0;
            mean = 0.0;
            m2 = 0.0;
            batchSize = 0;
        }
    }

    @Override
    public void update(int value) {
        LocalStats local = threadLocalStats.get();
        local.addValue(value);

        // Periodically merge into global stats
        if (local.shouldFlush()) {
            mergeToGlobal(local);
            local.batchSize = 0;
        }
    }

    private void mergeToGlobal(LocalStats local) {
        long stamp = lock.writeLock();
        try {
            // Merge using parallel mean/variance formulas
            long totalCount = globalCount + local.count;
            double delta = local.mean - globalMean;

            globalMean = (globalCount * globalMean + local.count * local.mean) / totalCount;
            globalM2 = globalM2 + local.m2 + delta * delta * globalCount * local.count / totalCount;
            globalCount = totalCount;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public float getVariance() {
        // Flush all pending thread-local data
        flushAllThreadLocals();

        long stamp = lock.tryOptimisticRead();
        long currentCount = globalCount;
        double currentM2 = globalM2;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentCount = globalCount;
                currentM2 = globalM2;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        if (currentCount <= 1) {
            return 0.0f;
        }

        return (float) (currentM2 / currentCount);
    }

    @Override
    public float getMean() {
        // Flush all thread-local stats first
        flushAllThreadLocals();

        long stamp = lock.tryOptimisticRead();
        double result = globalMean;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = globalMean;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return (float) result;
    }

    /**
     * Flush all thread-local stats to global stats.
     * This must be called before reading to get accurate results.
     */
    private void flushAllThreadLocals() {
        // Iterate through all registered thread-locals and merge them
        for (Map.Entry<Thread, LocalStats> entry : allThreadLocals.entrySet()) {
            LocalStats local = entry.getValue();

            // Merge if there's any pending data
            if (local.batchSize > 0) {
                mergeToGlobal(local);
                local.reset();
            }
        }

        // Clean up dead threads periodically
        allThreadLocals.keySet().removeIf(thread -> !thread.isAlive());
    }

    @Override
    public long getCount() {
        // Flush all pending thread-local data
        flushAllThreadLocals();

        long stamp = lock.tryOptimisticRead();
        long currentCount = globalCount;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentCount = globalCount;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return currentCount;
    }
}

class WelfordMeanAndVarianceThreadLocalLockFreeStrategy implements MeanAndVarianceCalculationStrategy {

    private final ThreadLocal<LocalStats> threadLocalStats = ThreadLocal.withInitial(LocalStats::new);

    private final ConcurrentLinkedQueue<LocalStats> pendingMerges = new ConcurrentLinkedQueue<>();

    // Global stats updated via CAS
    private final AtomicReference<GlobalStats> globalStats =
            new AtomicReference<>(new GlobalStats(0, 0.0, 0.0));

    private static class GlobalStats {
        final long count;
        final double mean;
        final double m2;

        GlobalStats(long count, double mean, double m2) {
            this.count = count;
            this.mean = mean;
            this.m2 = m2;
        }
    }

    private static class LocalStats {
        volatile long count = 0;
        volatile double mean = 0.0;
        volatile double m2 = 0.0;
        int batchSize = 0;
        static final int BATCH_THRESHOLD = 1000;

        void addValue(int value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
            batchSize++;
        }

        boolean shouldFlush() {
            return batchSize >= BATCH_THRESHOLD;
        }

        LocalStats snapshot() {
            LocalStats copy = new LocalStats();
            copy.count = this.count;
            copy.mean = this.mean;
            copy.m2 = this.m2;
            return copy;
        }

        void reset() {
            count = 0;
            mean = 0.0;
            m2 = 0.0;
            batchSize = 0;
        }
    }

    @Override
    public void update(int value) {
        LocalStats local = threadLocalStats.get();
        local.addValue(value);

        if (local.shouldFlush()) {
            pendingMerges.offer(local.snapshot());
            local.reset();
        }
    }

    private void flushAllThreadLocals() {
        LocalStats pending;
        while ((pending = pendingMerges.poll()) != null) {
            mergeToGlobalCAS(pending);
        }
    }

    private void mergeToGlobalCAS(LocalStats local) {
        while (true) {
            GlobalStats current = globalStats.get();

            long newCount = current.count + local.count;
            double delta = local.mean - current.mean;
            double newMean = (current.count * current.mean + local.count * local.mean) / newCount;
            double newM2 = current.m2 + local.m2 +
                    delta * delta * current.count * local.count / newCount;

            GlobalStats updated = new GlobalStats(newCount, newMean, newM2);

            if (globalStats.compareAndSet(current, updated)) {
                break;
            }
            // CAS failed, retry
        }
    }

    @Override
    public float getMean() {
        flushAllThreadLocals();
        return (float) globalStats.get().mean;
    }

    @Override
    public float getVariance() {
        flushAllThreadLocals();
        GlobalStats current = globalStats.get();
        return current.count <= 1 ? 0.0f : (float) (current.m2 / current.count);
    }

    @Override
    public long getCount() {
        flushAllThreadLocals();
        return globalStats.get().count;
    }
}

class SingleWriterStatistics implements Statistics {

    private static final int BUFFER_SIZE = 1024;

    // Padded slot to ensure each entry gets its own cache line
    @jdk.internal.vm.annotation.Contended
    private static class PaddedValue {
        volatile int value;

        // Explicit padding (alternative to @Contended if not available)
        // Cache line is 64 bytes, int is 4 bytes, object header ~16 bytes
        // Need ~44 bytes of padding
        private long p1, p2, p3, p4, p5, p6;  // 6 * 8 = 48 bytes padding

        PaddedValue(int value) {
            this.value = value;
        }
    }

    private final PaddedValue[] eventBuffer = new PaddedValue[BUFFER_SIZE];

    // These fields are accessed by multiple threads - pad them too!
    private final AtomicLong writeSequence = new AtomicLong(-1);
    private final AtomicLong readSequence = new AtomicLong(-1);

    // Stats read by multiple threads - pad these as well
    @jdk.internal.vm.annotation.Contended
    private volatile int min = 0;

    @jdk.internal.vm.annotation.Contended
    private volatile int max = 0;

    @jdk.internal.vm.annotation.Contended
    private volatile float mean = 0.0f;

    @jdk.internal.vm.annotation.Contended
    private volatile float variance = 0.0f;

    // Welford's algorithm state (accessed only by single processor thread)
    // These DON'T need padding since only one thread writes
    private long count = 0;
    private double m = 0.0;
    private double m2 = 0.0;

    public SingleWriterStatistics() {
        // Initialize padded buffer
        for (int i = 0; i < BUFFER_SIZE; i++) {
            eventBuffer[i] = new PaddedValue(0);
        }

        // Start background processor thread
        Thread processor = new Thread(this::processEvents, "stats-processor");
        processor.setDaemon(true);
        processor.start();
    }

    @Override
    public void event(int id, int n) {
        // Multi-producer: publish event to ring buffer
        long sequence = writeSequence.incrementAndGet();
        int index = (int) (sequence & (BUFFER_SIZE - 1));
        eventBuffer[index].value = n;  // No false sharing!
    }

    private void processEvents() {
        while (true) {
            long nextRead = readSequence.get() + 1;

            // Wait for available event
            while (writeSequence.get() < nextRead) {
                Thread.onSpinWait();
            }

            int index = (int) (nextRead & (BUFFER_SIZE - 1));
            int value = eventBuffer[index].value;

            // Update stats
            updateMin(value);
            updateMax(value);
            updateMeanVariance(value);

            readSequence.set(nextRead);
        }
    }

    private void updateMin(int value) {
        if (count == 1 || value < min) {
            min = value;
        }
    }

    private void updateMax(int value) {
        if (count == 1 || value > max) {
            max = value;
        }
    }

    private void updateMeanVariance(int value) {
        count++;
        double delta = value - m;
        m += delta / count;
        double delta2 = value - m;
        m2 += delta * delta2;

        mean = (float) m;
        variance = count > 1 ? (float) (m2 / count) : 0.0f;
    }

    @Override
    public int min() { return min; }

    @Override
    public int max() { return max; }

    @Override
    public float mean() { return mean; }

    @Override
    public float variance() { return variance; }
}