package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.ThreadState;
import org.junit.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

public class ActorSchedulerEntryPointTest {

    public static RuntimeState emptyRuntimeState() {
        return new RuntimeState() {
            @Override
            public CheckpointRegister checkpointRegister() {
                return null;
            }

            @Override
            public int monitorIdFor(Object object) {
                return 0;
            }

            @Override
            public int lockIdFor(Lock lock) {
                return 0;
            }

            @Override
            public Collection<? extends ThreadState> allActors() {
                return null;
            }

            @Override
            public Collection<ManagedThread> start(Object testInstance, Duration timeout) throws InterruptedException, TimeoutException {
                return null;
            }


            @Override
            public RuntimeState advance(ThreadState selected, Duration maxWaitTime) throws InterruptedException, TimeoutException {
                return null;
            }
        };
    }

    @Test
    public void testScheduler() {
    }

}