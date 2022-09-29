package concurrencytest.runtime;

import concurrencytest.runner.CheckpointReachedCallback;

public interface CheckpointRuntime {
//
//    boolean checkActualDispatchForMonitor(Object callTarget, String methodName, String methodDescription, long checkpointId, Supplier<Checkpoint> checkpointSupplier);
//
//    boolean checkActualDispatchForStaticMethod(Class<?> callTarget, String methodName, String methodDescription, long checkpointId, Supplier<Checkpoint> checkpointSupplier);
//
//    void beforeMonitorAcquiredCheckpoint(Object monitor, int id);
//
//    void afterMonitorReleasedCheckpoint(Object monitor, int id);
//
//    void beforeLockAcquiredCheckpoint(Lock lock, long id, Supplier<Checkpoint> checkpointSupplier);
//
//    void afterLockReleasedCheckpoint(Lock lock, long id, Supplier<Checkpoint> checkpointSupplier);

    void beforeActorStartCheckpoint();

    void actorFinishedCheckpoint();

    void checkpointReached(int id);

    void checkpointReached(int id, Object context);

    void fieldAccessCheckpoint(int checkpointId, Object owner, Object value);

    void addCheckpointCallback(CheckpointReachedCallback basicRuntimeState);

    void removeCheckpointCallback(CheckpointReachedCallback basicRuntimeState);
}
