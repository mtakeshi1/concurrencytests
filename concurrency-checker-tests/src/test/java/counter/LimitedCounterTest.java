package counter;

import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.config.*;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(ActorSchedulerRunner.class)
public class LimitedCounterTest {

    //region magic incantation
    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(LimitedCounterTest.class) {

            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(LimitedCounter.class, LimitedCounterFixed.class);
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return List.of();
                    }

                    @Override
                    public Collection<FieldAccessMatch> fieldsToInstrument() {
                        return List.of();
                    }


                    @Override
                    public Collection<MethodInvocationMatcher> methodsCallsToInstrument() {
                        return List.of((classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
                                invocationTargetType == LimitedCounter.class && injectionPoint == InjectionPoint.AFTER,
                                (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
                                        invocationTargetType == ConcurrentLimitedCounter.class && injectionPoint == InjectionPoint.AFTER);
                    }

                    @Override
                    public boolean includeStandardMethods() {
                        return false;
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

    public static final int NUM_THREADS = 2;
    public static final int LOOP = 2;
    public static final int LIMIT = 2;

    private ConcurrentLimitedCounter limitedCounter;

    private final AtomicInteger[] frequencies = new AtomicInteger[LIMIT + 1];

    @Before
    public void setup() {
        limitedCounter = new LimitedCounter(LIMIT);
//        limitedCounter = new LimitedCounterFixed(LIMIT);
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = new AtomicInteger();
        }
    }

    @MultipleActors(numberOfActors = NUM_THREADS)
    public void actor() {
        for (int i = 0; i < LOOP; i++) {
            for (int j = 0; j <= LIMIT; j++) {
                int x = limitedCounter.inc();
                Assert.assertTrue("x should be >= 0 but was: " + x, x >= 0);
                Assert.assertTrue("x should be <= " + LIMIT + " but was: " + x, x <= LIMIT);
                frequencies[x].incrementAndGet();
            }
        }
    }

    @AfterActorsCompleted
    public void noMissedIncrement() {
        for (int i = 0; i < frequencies.length; i++) {
            System.out.printf("value: %d, freq: %d %n", i, frequencies[i].get());
        }
        for (int i = 0; i < frequencies.length; i++) {
            Assert.assertEquals("value: %d - count: %d expected: %d".formatted(i, frequencies[i].get(), NUM_THREADS * LOOP), NUM_THREADS * LOOP, frequencies[i].get());
        }
    }

}
