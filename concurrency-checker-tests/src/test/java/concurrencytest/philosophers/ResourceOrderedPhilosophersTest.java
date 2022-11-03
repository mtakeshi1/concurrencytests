package concurrencytest.philosophers;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
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

@RunWith(ActorSchedulerRunner.class)
public class ResourceOrderedPhilosophersTest {

    private static final int NUM = 3;

    private Spoon[] spoons;

    private boolean[] eaten;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(ResourceOrderedPhilosophersTest.class) {
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
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public int parallelExecutions() {
                return 4;
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

    @Actor
    public void philo0() {
        philosopher(0);
    }

    @Actor
    public void philo2() {
        philosopher(2);
    }

    @Actor
    public void philo1() {
        philosopher(1);
    }

    public void philosopher(int index) {
        int other = (index + 1) % spoons.length;
        Spoon left = spoons[Math.min(index, other)];
        Spoon right = spoons[Math.max(index, other)];
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
