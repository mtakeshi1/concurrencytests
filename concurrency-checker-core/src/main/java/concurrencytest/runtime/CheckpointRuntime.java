package concurrencytest.runtime;

import concurrencytest.runner.CheckpointReachedCallback;

import java.util.Optional;

public interface CheckpointRuntime {

    void beforeActorStartCheckpoint();

    void actorFinishedCheckpoint();

    void checkpointReached(int id);

    void checkpointReached(int id, Object context);

    void addCheckpointCallback(CheckpointReachedCallback basicRuntimeState);

    void removeCheckpointCallback(CheckpointReachedCallback basicRuntimeState);

    void reportError(Throwable throwable);

    Optional<Throwable> errorReported();

}
