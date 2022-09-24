package concurrencytest.runner;

import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.ManagedThread;

import java.util.concurrent.ScheduledExecutorService;

public interface ActorBuilder {

    ManagedThread build(CheckpointRuntime runtime, ScheduledExecutorService managedExecutorService, Object testInstance);

}
