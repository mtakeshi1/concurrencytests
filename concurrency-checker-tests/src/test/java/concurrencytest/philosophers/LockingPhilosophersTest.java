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

//            @Override
//            public Collection<Class<?>> classesToInstrument() {
//                return List.of(Spoon.class);
//            }
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

    @Actor
    public void philo0() {
        philosopher(0);
    }

//    @Actor
//    public void philo2() {
//        philosopher(2);
//    }

    @Actor
    public void philo1() {
        philosopher(1);
    }

    public void philosopher(int index) {
        var left = spoons[index];
        var right = spoons[(index+1) % spoons.length];
        while (true) {
            if(left.tryLock()) {
                try {
                    if(right.tryLock()) {
                        eaten[index] = true;
                        right.unlock();
                        return;
                    }
                } finally {
                    left.unlock();
                }
            }
        }
    }

    @Invariant
    public void spoonsShouldBeUsedByOne() {
//        for (Spoon spoon : spoons) {
//            Assert.assertTrue(spoon.getUsed() <= 1);
//        }
    }

    @AfterActorsCompleted
    public void allEaten() {
        for (boolean satisfied : eaten) {
            Assert.assertTrue(satisfied);
        }
    }

}
