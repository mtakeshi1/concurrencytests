package concurrencytest.runtime;

import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A managed thread is a thread that upon running, will install a CheckpointRuntime if available and invoke checkpoint callbacks.
 */
public class ManagedThread extends Thread {

    private volatile CheckpointRuntime checkpointRuntime = CheckpointRuntimeAccessor.getCheckpointRuntime();

    private volatile String actorName;

    private final Thread parentThread = Thread.currentThread();

    private final AtomicInteger childIndex = new AtomicInteger();

    public ManagedThread(Runnable target, CheckpointRuntime runtime, String actorName) {
        super(null, target, "actor_thread_for_" + actorName);
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
        String actorName = getActorName();
        MDC.put("actor", actorName);
        CheckpointRuntimeAccessor.associateRuntime(this.checkpointRuntime);
        try {
            CheckpointRuntimeAccessor.beforeStartCheckpoint();
            super.run();
        } finally {
            CheckpointRuntimeAccessor.releaseRuntime();
            MDC.remove("actor");
        }
    }

    private String newChildActorName() {
        return this.actorName + "_" + childIndex.getAndIncrement();
    }

    public CheckpointRuntime getCheckpointRuntime() {
        return checkpointRuntime;
    }

    public void setCheckpointRuntime(CheckpointRuntime checkpointRuntime) {
        this.checkpointRuntime = checkpointRuntime;
    }

    public synchronized String getActorName() {
        if (actorName == null && parentThread instanceof ManagedThread mt) {
            actorName = mt.newChildActorName();
        }
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public AtomicInteger getChildIndex() {
        return childIndex;
    }
}
