package counter;

import concurrencytest.annotations.*;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.config.*;
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

    public static final int INCREMENTS = 1;

    private ConcurrentCounter counter;

    private int[] observed;

    private int lastSeen;

    //region magic incantation
    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(ConcurrentCounterTest.class) {
            @Override
            public CheckpointDurationConfiguration durationConfiguration() {
                return new CheckpointDurationConfiguration(Duration.ofMinutes(1), Duration.ofMinutes(1), Duration.ofMinutes(60));
            }

            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(VolatileCounter.class, AtomicCounter.class);
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return List.of();
                    }

                    @Override
                    public Collection<MethodInvocationMatcher> methodsCallsToInstrument() {
//                        return List.of((classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
//                                        methodName.equals("inc") && injectionPoint == InjectionPoint.AFTER);
                        return List.of();
                    }
                };
            }

            @Override
            public List<? extends String> startingPath() {
//                return List.of("actor_1", "actor_0", "actor_1", "actor_1", "actor_1", "actor_0", "actor_0", "actor_0", "actor_0");
                return List.of();
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
//        counter = new AtomicCounter();
        observed = new int[NUM_ACTORS];
    }

    @MultipleActors(numberOfActors = NUM_ACTORS)
    public void actor(int index) {
        for (int i = 0; i < INCREMENTS; i++) {
            counter.inc();
            observed[index] = counter.get();
        }
        observed[index] = counter.get();
    }

    @Invariant
    public void monotonicCounter() {
        int c = counter.get();
        Assert.assertTrue("counter has to be monotonic. Current: %d, lastseen %d".formatted(c, lastSeen), c >= lastSeen);
        lastSeen = c;
    }

    @Invariant
    public void valueGreaterThanObserved() {
        int c = counter.get();
        for (var ob : observed) {
            Assert.assertTrue(c >= ob);
        }
    }

    @AfterActorsCompleted
    public void finalCounterValue() {
//        Assert.assertEquals(counter.get(), Arrays.stream(observed).max().orElseThrow());
        Assert.assertEquals(NUM_ACTORS * INCREMENTS, counter.get());
    }


}
