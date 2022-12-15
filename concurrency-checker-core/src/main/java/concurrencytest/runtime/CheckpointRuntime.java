package concurrencytest.runtime;

import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runner.CheckpointReachedCallback;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A checkpoint runtime is the glue that connects actors, checkpoints and a RuntimeState.
 * Actor threads should call methods on this so the proper callbacks are invoked
 */
public interface CheckpointRuntime {

    /**
     * Called before an actor starts
     */
    void beforeActorStartCheckpoint();

    /**
     * Called as an actor is about to finish, tipically in a finally block
     */
    void actorFinishedCheckpoint();

    /**
     * Signals that the current ManagedThread checked a checkpoint
     *
     * @param id the checkpoint id
     */
    void checkpointReached(int id);

    /**
     * Signals that the current ManagedThread checked a checkpoint, carrying context information to aid
     *
     * @param id      the checkpoint id
     * @param context a context object that depends on the type of the checkpoint
     */
    void checkpointReached(int id, Object context);

    /**
     * Adds a callback to be called when an actor reaches a checkpoint.
     * Currently, the actor reaching the checkpoints invokes the callback, but I think it should be run by the scheduler thread
     *
     * @param callback the callback
     */
    void addCheckpointCallback(CheckpointReachedCallback callback);

    default <T extends CheckpointReached> void addTypedCheckpointCallback(Class<T> type, Consumer<T> consumer) {
        addCheckpointCallback(cb -> {if (type.isInstance(cb)) consumer.accept(type.cast(cb));});
    }

    /**
     * Removes a checkpoint callback. Not sure if this is needed
     */
    void removeCheckpointCallback(CheckpointReachedCallback basicRuntimeState);

    /**
     * Report that an error has occured inside an actor method
     *
     * @param throwable the error
     */
    void reportError(Throwable throwable);

    /**
     * In an error is reported by {@link CheckpointRuntime#reportError(Throwable)} it will be contained here
     */
    Optional<Throwable> errorReported();

}
