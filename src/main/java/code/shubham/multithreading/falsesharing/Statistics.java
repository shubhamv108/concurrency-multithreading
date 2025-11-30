package multithreading.falsesharing;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

public interface Statistics {
    /**
     * The only method which takes events as input.
     * @param n
     */
    void event(int n);
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
    public void event(int n) {
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

@jdk.internal.vm.annotation.Contended
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

@jdk.internal.vm.annotation.Contended
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
