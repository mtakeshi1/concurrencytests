package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.*;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.time.Duration;

public class SynchronizedMethodCounter {

    private volatile int counter;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(SynchronizedMethodCounter.class) {

            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public boolean monitorCheckpointEnabled() {
                        return true;
                    }

                    @Override
                    public boolean removeSynchronizedMethodDeclaration() {
                        return true;
                    }
                };
            }

            @Override
            public int parallelExecutions() {
                return 2;
            }

            @Override
            public CheckpointDurationConfiguration durationConfiguration() {
                return new CheckpointDurationConfiguration(Duration.ofMinutes(20), Duration.ofMinutes(30), Duration.ofHours(1));
            }
        };
    }

    @Actor
    public synchronized void actor1() {
        counter++;
    }

    @Actor
    public synchronized void actor2() {
        counter++;
    }

//    @Actor
//    public synchronized void actor3() {
//        counter++;
//    }

//    @Actor
//    public synchronized void actor4() {
//        counter++;
//    }

    @AfterActorsCompleted
    public void finished() {
        Assert.assertEquals(2, counter);
    }

}
