package concurrencytest.util;

import java.util.concurrent.atomic.AtomicLong;

public class LongStatistics {

    private final AtomicLong sum = new AtomicLong(), sampleCount = new AtomicLong();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE), max = new AtomicLong(Long.MIN_VALUE);

    public void newSample(long value) {
        sum.addAndGet(value);
        sampleCount.incrementAndGet();
        updateMin(value);
        updateMax(value);
    }

    public double getAverage() {
        double n = sampleCount.get();
        return sum.get() / n;
    }

    public long getSampleCount() {
        return sampleCount.get();
    }

    public long getMin() {
        if (sampleCount.get() == 0) {
            return 0;
        }
        return min.get();
    }

    public long getMax() {
        if (sampleCount.get() == 0) {
            return 0;
        }
        return max.get();
    }


    public String toString() {
        return "Average: " + getAverage() + ", n = " + getSampleCount() + ", min: " + getMin() + ", max: " + getMax();
    }

    private void updateMax(long value) {
        while (true) {
            if (max.get() < value) {
                if (max.compareAndSet(max.get(), value)) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private void updateMin(long value) {
        while (true) {
            if (min.get() > value) {
                if (min.compareAndSet(min.get(), value)) {
                    break;
                }
            } else {
                break;
            }
        }
    }


}
