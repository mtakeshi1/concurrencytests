package concurrencytest;

import concurrencytest.checkpoint.CheckpointImpl;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ManagedThread extends Thread {

    private final TestRuntimeImpl testRuntime;
    private int loopCount;
    private CheckpointImpl checkpoint;
    private final Object threadIdentification;
    private boolean allowedToContinue;

    private LList<CheckpointImpl> reachedCheckpoints = LList.empty();

    //TODO record monitor / lock states
    private String waitingForMonitorOrLock;
    private List<String> ownedLocksAndMonitors;

    public ManagedThread(ThreadGroup group, Runnable task, String threadIdentification, TestRuntimeImpl runtime) {
        super(group, task, threadIdentification);
        this.threadIdentification = threadIdentification;
        this.testRuntime = runtime;
    }

    @Override
    public void run() {
        TestRuntimeImpl.setCurrentInstance(this.testRuntime);
        super.run();
    }

    public int getLoopCount() {
        return loopCount;
    }

    public synchronized CheckpointImpl getCheckpoint() {
        return checkpoint;
    }

    public synchronized void waitForCheckpoint(int maxWaitRendezvousSeconds) throws InterruptedException {
        long ts = System.nanoTime() + TimeUnit.SECONDS.toNanos(maxWaitRendezvousSeconds);
        while (checkpoint == null && this.isAlive() && System.nanoTime() < ts) {
            wait(100);
        }
    }

    public LList<CheckpointImpl> getReachedCheckpoints() {
        return reachedCheckpoints;
    }

    public synchronized void setCheckpointAndWait(CheckpointImpl checkpoint) {
        if (testRuntime.isDisabled()) {
            return;
        }
        this.allowedToContinue = false;

        if (this.checkpoint == checkpoint) {
            loopCount++;
        } else {
            loopCount = 0;
        }
        this.checkpoint = checkpoint;
        this.reachedCheckpoints = reachedCheckpoints.prepend(checkpoint);
        notifyAll();
        while (!allowedToContinue && !testRuntime.isDisabled()) {
            try {
                this.wait(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (testRuntime.isDisabled()) {
            throw ThreadDisabled.INSTANCE;
        }
    }

    public synchronized void resumeCheckpoint() {
        this.checkpoint = null;
        this.allowedToContinue = true;
        notifyAll();
    }

    public boolean canAdvance() {
        return isAlive() && (getState() == State.RUNNABLE || getState() == State.WAITING || getState() == State.TIMED_WAITING);
    }
}
