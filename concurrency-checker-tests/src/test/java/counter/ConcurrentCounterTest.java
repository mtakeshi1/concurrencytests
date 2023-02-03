package counter;

import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@RunWith(ActorSchedulerRunner.class)
public class ConcurrentCounterTest {

    public static final int NUM_ACTORS = 2;

    private ConcurrentCounter counter;

    private int[] observed;

    private int lastSeen;

    //region magic incantation
    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(ConcurrentCounterTest.class) {
            @Override
            public CheckpointDurationConfiguration durationConfiguration() {
                return new CheckpointDurationConfiguration(Duration.ofMinutes(1), Duration.ofMinutes(1), Duration.ofMinutes(1));
            }

            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(VolatileCounter.class, SyncCounter.class);
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return List.of();
                    }
                };
            }

            @Override
            public int parallelExecutions() {
                return 1;
            }
        };
    }
    //endregion

    @Before
    public void before() {
        counter = new VolatileCounter();
        observed = new int[NUM_ACTORS];
    }

    @MultipleActors(numberOfActors = NUM_ACTORS)
    public void actor(int index) {
        counter.inc();
        Assert.assertTrue(counter.get() >= 1);
//        counter.inc();
//        Assert.assertTrue(counter.get() >= 2);
        observed[index] = counter.get();
    }

    @Invariant
    public void monotonicCounter() {
        int c = counter.get();
        Assert.assertTrue("counter has to be monotonic. Current: %d, lastseen %d".formatted(c, lastSeen), c >= lastSeen);
        lastSeen = c;
    }


    @AfterActorsCompleted
    public void finalCounterValue() {
//        Assert.assertEquals(counter.get(), Arrays.stream(observed).max().orElseThrow());
        Assert.assertEquals(2, counter.get());
    }


}
