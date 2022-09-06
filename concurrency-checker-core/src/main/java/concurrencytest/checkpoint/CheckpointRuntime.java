package concurrencytest.checkpoint;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public interface CheckpointRuntime {
//
//    boolean checkActualDispatchForMonitor(Object callTarget, String methodName, String methodDescription, long checkpointId, Supplier<Checkpoint> checkpointSupplier);
//
//    boolean checkActualDispatchForStaticMethod(Class<?> callTarget, String methodName, String methodDescription, long checkpointId, Supplier<Checkpoint> checkpointSupplier);
//
//    void beforeMonitorAcquiredCheckpoint(Object monitor, long id, Supplier<Checkpoint> checkpointSupplier);
//
//    void afterMonitorReleasedCheckpoint(Object monitor, long id, Supplier<Checkpoint> checkpointSupplier);
//
//    void beforeLockAcquiredCheckpoint(Lock lock, long id, Supplier<Checkpoint> checkpointSupplier);
//
//    void afterLockReleasedCheckpoint(Lock lock, long id, Supplier<Checkpoint> checkpointSupplier);

    void checkpointReached(long id);

    void fieldAccessCheckpoint(long checkpointId, Object owner, Object value);
}
