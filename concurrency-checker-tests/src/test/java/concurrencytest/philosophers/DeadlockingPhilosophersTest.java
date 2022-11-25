package concurrencytest.philosophers;

import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(ActorSchedulerRunner.class)
public class DeadlockingPhilosophersTest {

    private static final int NUM = 2;

    private Spoon[] spoons;

    private boolean[] eaten;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(DeadlockingPhilosophersTest.class) {
            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return Collections.emptyList();
                    }
                };
            }

            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(DeadlockingPhilosophersTest.class, Spoon.class);
            }

            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }
        };
    }

    @Before
    public void setup() {
        spoons = new Spoon[NUM];
        eaten = new boolean[NUM];
        for (int i = 0; i < NUM; i++) {
            spoons[i] = new Spoon();
        }
    }

    @MultipleActors(numberOfActors = NUM)
    public void philosopher(int index) {
        Spoon left = spoons[index];
        Spoon right = spoons[(index + 1) % spoons.length];
        synchronized (left) {
            left.pickup();
            synchronized (right) {
                right.pickup();
                eaten[index] = true;
                right.putDown();
            }
            left.putDown();
        }
    }

    @Invariant
    public void spoonsShouldBeUsedByOne() {
        for (Spoon spoon : spoons) {
            Assert.assertTrue(spoon.getUsed() <= 1);
        }
    }

    @AfterActorsCompleted
    public void allEaten() {
        for (boolean satisfied : eaten) {
            Assert.assertTrue(satisfied);
        }
    }

}
