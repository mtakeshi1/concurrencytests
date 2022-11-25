package scheduledjobs;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(ActorSchedulerRunner.class)
public class ScheduledJobsStamp {

    private final AtomicReference<BlockableQueue> queueAtomicReference = new AtomicReference<>(new BlockableQueue());

    private final AtomicInteger count = new AtomicInteger();

    public void actorAction() {
        while (true) {
            BlockableQueue old = queueAtomicReference.get();
            if (old.add("action")) {
                if (queueAtomicReference.compareAndSet(old, new BlockableQueue())) {
                    old.block();
                    while (!old.queue().isEmpty()) {
                        var element = old.queue().poll();
                        Assert.assertNotNull(element);
                        count.incrementAndGet();
                    }
                }
                break;
            }
        }

    }

    @Invariant
    public void neverBlocked() {
        Assert.assertFalse(queueAtomicReference.get().blocked().get());
    }

    @Actor
    public void actor1() {
        actorAction();
    }

    @Actor
    public void actor2() {
        actorAction();
    }

    @AfterActorsCompleted
    public void countCompleted() {
        Assert.assertEquals(2, count.get());
    }

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(ScheduledJobsStamp.class) {
            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public int parallelExecutions() {
                return 8;
            }

            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(BlockableQueue.class, ScheduledJobsStamp.class);
            }
        };
    }


}
