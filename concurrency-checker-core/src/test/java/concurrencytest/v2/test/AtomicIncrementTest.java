package concurrencytest.v2.test;

import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.AfterActorsCompleted;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIncrementTest {

    public static final int NUM_THREADS = 2;

    private final AtomicInteger count = new AtomicInteger();

    @MultipleActors(numberOfActors = NUM_THREADS)
    public void run() {
        count.incrementAndGet();
    }

    @AfterActorsCompleted
    public void sanity() {
        Assert.assertEquals(NUM_THREADS, count.get());
    }


}
