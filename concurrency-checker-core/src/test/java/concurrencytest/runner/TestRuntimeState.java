package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.runtime.thread.ThreadState;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public record TestRuntimeState(CheckpointRegister checkpointRegister, Collection<? extends ThreadState> allActors) implements RuntimeState {

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
}
