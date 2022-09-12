package concurrencytest;

import concurrencytest.runtime.CheckpointRuntime;

public class CheckpointRuntimeAccessor {

    private static final ThreadLocal<CheckpointRuntime> runtimeThreadLocal = new ThreadLocal<>();

    public static void fieldAccessCheckpoint(int checkpointId, Object owner, Object value) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.fieldAccessCheckpoint(checkpointId, owner, value);
        }

    }

    public static void manualCheckpoint() {
    }

    public static void manualCheckpoint(String details) {
    }

    public static void genericCheckpointReached(Object context, int checkpointId) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.checkpointReached(checkpointId, context);
        }
    }

    public static void checkpointWithMessageReached(String details, int checkpointId) {
        genericCheckpointReached(details, checkpointId);
    }

    public static void checkpointReached(int checkpointId) {
        genericCheckpointReached("", checkpointId);
    }

    public static void setup(CheckpointRuntime runtime) {
        runtimeThreadLocal.set(runtime);
    }
}
