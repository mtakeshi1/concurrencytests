package concurrencytest.v2.test;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.time.Duration;

@RunWith(ActorSchedulerRunner.class)
public class SynchronizedMethodCounter {

    private volatile int counter;

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(SynchronizedMethodCounter.class) {
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

    @AfterActorsCompleted
    public void finished() {
        Assert.assertEquals(2, counter);
    }

}
