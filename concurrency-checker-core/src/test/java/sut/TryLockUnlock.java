package sut;

import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RunWith(ActorSchedulerRunner.class)
public class TryLockUnlock {

    public static final int NUM_ACTORS = 2;

    private final Lock left = new ReentrantLock();

    private volatile int count;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(TryLockUnlock.class) {
            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }
        };
    }

    @MultipleActors(numberOfActors = NUM_ACTORS)
    public void actors() {
        if (left.tryLock()) {
            try {
                count++;
            } finally {
                left.unlock();
            }
        } else {
            System.out.println("failed to acquire");
        }
    }

    @AfterActorsCompleted
    public void checkCount() {
        Assert.assertEquals(NUM_ACTORS, count);
    }

}
