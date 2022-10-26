package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.thread.ThreadState;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.Lock;

public record TestRuntimeState(CheckpointRegister checkpointRegister, Collection<? extends ThreadState> allActors) implements RuntimeState {

    public TestRuntimeState(ThreadState singleThread) {
        this(new StandardCheckpointRegister(), List.of(singleThread));
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
    public List<String> getExecutionPath() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ManagedThread> start(Object testInstance, Duration timeout) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public RuntimeState advance(ThreadState selected, Duration maxWaitTime) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Optional<Throwable> errorReported() {
        return Optional.empty();
    }

    public TestRuntimeState update(ThreadState locked) {
        Map<String, ThreadState> map = new HashMap<>();
        for (ThreadState ts : this.allActors()) {
            map.put(ts.actorName(), ts);
        }
        map.put(locked.actorName(), locked);
        return new TestRuntimeState(this.checkpointRegister, map.values());
    }
}
