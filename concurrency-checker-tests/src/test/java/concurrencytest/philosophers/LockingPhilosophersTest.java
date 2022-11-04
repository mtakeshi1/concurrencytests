package concurrencytest.philosophers;

import concurrencytest.annotations.MultipleActors;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RunWith(ActorSchedulerRunner.class)
public class LockingPhilosophersTest {

    private static final int NUM = 2;

    private Lock[] spoons;

    private boolean[] eaten;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(LockingPhilosophersTest.class) {

            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

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
            public int parallelExecutions() {
                return Runtime.getRuntime().availableProcessors();
            }

            @Override
            public int maxLoopIterations() {
                return 2;
            }
        };
    }

    @Before
    public void setup() {
        spoons = new Lock[NUM];
        eaten = new boolean[NUM];
        for (int i = 0; i < NUM; i++) {
            spoons[i] = new ReentrantLock();
        }
    }

    @MultipleActors(numberOfActors = NUM)
    public void philosopher(int index) {
        var left = spoons[index];
        var right = spoons[(index + 1) % spoons.length];
        while (!eaten[index]) {
            if (left.tryLock()) {
                if (right.tryLock()) {
                    eaten[index] = true;
                    right.unlock();
                }
                left.unlock();
            }
        }
    }

    @AfterActorsCompleted
    public void allEaten() {
        for (boolean satisfied : eaten) {
            Assert.assertTrue(satisfied);
        }
    }

}
