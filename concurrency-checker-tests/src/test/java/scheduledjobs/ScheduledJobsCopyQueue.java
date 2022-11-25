package scheduledjobs;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(ActorSchedulerRunner.class)
public class ScheduledJobsCopyQueue {

    private final AtomicInteger sendCount = new AtomicInteger();

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(ScheduledJobsCopyQueue.class) {
            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }
        };
    }

    public record QueueWithBoolean(Queue<String> queue) {
        public QueueWithBoolean() {
            this(new ArrayBlockingQueue<>(10));
        }
    }

    private final AtomicReference<QueueWithBoolean> queueRef = new AtomicReference<>(new QueueWithBoolean(new ArrayBlockingQueue<>(1024)));

    @Actor
    public void actor1() {
        actorAction();
    }

    public void actorAction() {
        var old = queueRef.get();
        old.queue().add("a");
        if (queueRef.compareAndSet(old, new QueueWithBoolean())) {
            while (!old.queue().isEmpty()) {
                String poll = old.queue().poll();
                Assert.assertNotNull(poll);
                sendCount.incrementAndGet();
            }
        }
    }

    @Actor
    public void actor2() {
        actorAction();
    }

//    @Actor
    public void actor3() {
        actorAction();
    }


    @AfterActorsCompleted
    public void queueEmpty() {
        QueueWithBoolean queue = queueRef.get();
        Assert.assertNotNull(queue);
        Assert.assertTrue(queue.queue().isEmpty());
        Assert.assertEquals(2, sendCount.get());
    }


}
