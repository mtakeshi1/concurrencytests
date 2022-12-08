package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.RegularCheckpointReached;
import concurrencytest.runtime.exception.RunAbortedException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class StandardCheckpointRuntime implements CheckpointRuntime {

    private final CheckpointRegister checkpointRegister;

    private final List<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    private volatile Throwable error;

    public StandardCheckpointRuntime(CheckpointRegister checkpointRegister) {
        this.checkpointRegister = checkpointRegister;
    }

    @Override
    public void beforeActorStartCheckpoint() {
        invokeCallbacks(checkpointRegister.taskStartingCheckpoint().newCheckpointReached("", Thread.currentThread()));
    }

    @Override
    public void actorFinishedCheckpoint() {
        Checkpoint checkpoint = checkpointRegister.taskFinishedCheckpoint();
        invokeCallbacks(checkpoint.newCheckpointReached("actor_finished", Thread.currentThread()));
    }

    @Override
    public void checkpointReached(int id) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        invokeCallbacks(new RegularCheckpointReached(checkpoint, "", Thread.currentThread()));
    }

    @Override
    public void checkpointReached(int id, Object details) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        invokeCallbacks(checkpoint.newCheckpointReached(details, Thread.currentThread()));
    }

    protected void invokeCallbacks(CheckpointReached newCheckpointReached) {
        for (var cb : callbacks) {
            try {
                cb.checkpointReached(newCheckpointReached);
            } catch (InterruptedException e) {
                // we bail
                Thread.currentThread().interrupt();
                throw new RunAbortedException();
            } catch (Exception e) {
                this.error = e;
            }
        }
    }


    @Override
    public void addCheckpointCallback(CheckpointReachedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCheckpointCallback(CheckpointReachedCallback basicRuntimeState) {
        callbacks.remove(basicRuntimeState);
    }

    @Override
    public void reportError(Throwable throwable) {
        this.error = throwable;
    }

    @Override
    public Optional<Throwable> errorReported() {
        return Optional.ofNullable(error);
    }
}
