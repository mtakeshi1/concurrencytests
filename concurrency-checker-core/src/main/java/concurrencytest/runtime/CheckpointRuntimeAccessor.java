package concurrencytest.runtime;

import java.util.function.Supplier;

/**
 * Entrypoint for signalling checkpoints.
 * <p>
 * The methods here shouldn't be invoked by user code, except {@link CheckpointRuntimeAccessor#manualCheckpoint()} and {@link CheckpointRuntimeAccessor#manualCheckpoint(String)}
 * <p>
 * These methods are meant to be called by modified bytecode.
 */
public class CheckpointRuntimeAccessor {

    /**
     * Holds the CheckpointRuntime associated with the current thread, responsible for actually invoking the checkpoints
     */
    private static final ThreadLocal<CheckpointRuntime> runtimeThreadLocal = new ThreadLocal<>();

    /**
     * User code can use these two methods to inject checkpoints manually. They will be rewritten into the proper methods
     * by the bytecode enhancer, therefore these methods should impose almost no performance penalty when running
     * the plain version
     */
    public static void manualCheckpoint() {
    }

    /**
     * User code can use these two methods to inject checkpoints manually. They will be rewritten into the proper methods
     * by the bytecode enhancer, therefore these methods should impose almost no performance penalty when running
     * the plain version
     *
     * @param details details to be attached to the checkpoint to help identifying issues
     */
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

    public static CheckpointRuntime getCheckpointRuntime() {
        return runtimeThreadLocal.get();
    }

    public static void associateRuntime(CheckpointRuntime checkpointRuntime) {
        runtimeThreadLocal.set(checkpointRuntime);
    }

    public static void releaseRuntime() {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.actorFinishedCheckpoint();
            runtimeThreadLocal.remove();
        }
    }

    public static void beforeStartCheckpoint() {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime != null) {
            runtime.beforeActorStartCheckpoint();
        }
    }

    public static void ignoringCheckpoints(Runnable run) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime == null) {
            run.run();
            return;
        }
        try {
            runtimeThreadLocal.remove();
            run.run();
        } finally {
            runtimeThreadLocal.set(runtime);
        }
    }

    public static <T> T ignoringCheckpoints(Supplier<T> supplier) {
        CheckpointRuntime runtime = runtimeThreadLocal.get();
        if (runtime == null) {
            return supplier.get();
        }
        try {
            runtimeThreadLocal.remove();
            return supplier.get();
        } finally {
            runtimeThreadLocal.set(runtime);
        }
    }
}
