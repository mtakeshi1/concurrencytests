package concurrencytest;

import concurrencytest.checkpoint.CheckpointRuntime;

public class CheckpointRuntimeAccessor {

    private static final ThreadLocal<CheckpointRuntime> runtimeThreadLocal = new ThreadLocal<>();

    public static void fieldAccessCheckpoint(long checkpointId, Object owner, Object value) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.fieldAccessCheckpoint(checkpointId, owner, value);
        }

    }

    public static void checkpointReached(long checkpointId) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.checkpointReached(checkpointId);
        }
    }

    public static void setup(CheckpointRuntime runtime) {
        runtimeThreadLocal.set(runtime);
    }
}
