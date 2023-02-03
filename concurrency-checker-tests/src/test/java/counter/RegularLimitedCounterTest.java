package counter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class RegularLimitedCounterTest {


    public static final int NUM_THREADS = 1;
    public static final int LOOP = 3;
    public static final int LIMIT = 2;

    private final LimitedCounter limitedCounter = new LimitedCounter(LIMIT);

    private final AtomicInteger[] frequencies = new AtomicInteger[LIMIT + 1];

    @Before
    public void setup() {
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = new AtomicInteger();
        }
    }

    //    @MultipleActors(numberOfActors = NUM_THREADS)
    @Test
    public void actor() {
        for (int i = 0; i < LOOP; i++) {
            for (int j = 0; j <= LIMIT; j++) {
                int x = limitedCounter.inc();
                Assert.assertTrue(x >= 0);
                Assert.assertTrue(x <= LIMIT);
                frequencies[x].incrementAndGet();
            }
        }
        for (int i = 0; i < frequencies.length; i++) {
            Assert.assertEquals("value: %d - count: %d expected: %d".formatted(i, frequencies[i].get(), NUM_THREADS * LOOP), NUM_THREADS * LOOP, frequencies[i].get());
        }
    }

}
