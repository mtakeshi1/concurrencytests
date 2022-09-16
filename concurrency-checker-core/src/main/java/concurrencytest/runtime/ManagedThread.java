package concurrencytest.runtime;

import java.util.concurrent.atomic.AtomicInteger;

public class ManagedThread extends Thread {

    private volatile CheckpointRuntime checkpointRuntime = CheckpointRuntimeAccessor.getCheckpointRuntime();

    private volatile String actorName;

    private final AtomicInteger childIndex = new AtomicInteger();

    public ManagedThread(Runnable target, CheckpointRuntime runtime, String actorName) {
        super(target);
        this.checkpointRuntime = runtime;
        this.actorName = actorName;
    }

    public ManagedThread() {
    }

    public ManagedThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public ManagedThread(String name) {
        super(name);
    }

    public ManagedThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public ManagedThread(Runnable target, String name) {
        super(target, name);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public ManagedThread(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) {
        super(group, target, name, stackSize, inheritThreadLocals);
    }

    @Override
    public void run() {
        CheckpointRuntimeAccessor.associateRuntime(this.checkpointRuntime);
        try {
            CheckpointRuntimeAccessor.beforeStartCheckpoint();
            super.run();
        } finally {
            CheckpointRuntimeAccessor.releaseRuntime();
        }
    }

    public CheckpointRuntime getCheckpointRuntime() {
        return checkpointRuntime;
    }

    public void setCheckpointRuntime(CheckpointRuntime checkpointRuntime) {
        this.checkpointRuntime = checkpointRuntime;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public AtomicInteger getChildIndex() {
        return childIndex;
    }
}
