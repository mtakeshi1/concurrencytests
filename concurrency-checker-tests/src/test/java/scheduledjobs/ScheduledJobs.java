package scheduledjobs;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(ActorSchedulerRunner.class)
public class ScheduledJobs {

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(ScheduledJobs.class) {
            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {

                };
            }
        };
    }

    private final AtomicBoolean sending = new AtomicBoolean();

    private final Queue<String> queue = new ArrayBlockingQueue<>(1024); // we dont need it to be blocking

    @Actor
    public void actor1() {
        queue.add("a");
        CheckpointRuntimeAccessor.manualCheckpoint("after enqueued");
        consumeIfPossible();
    }

    private void consumeIfPossible() {
        if (sending.compareAndSet(false, true)) {
            while (!queue.isEmpty()) {
                String poll = queue.poll();
                Assert.assertNotNull(poll);
            }
            Assert.assertTrue(sending.compareAndSet(true, false));
        }
    }


    @Actor
    public void actor2() {
        queue.add("b");
        CheckpointRuntimeAccessor.manualCheckpoint("after enqueue(2)");
        consumeIfPossible();
    }


    @AfterActorsCompleted
    public void queueEmpty() {
        Assert.assertTrue(queue.isEmpty());
    }


}
