package concurrencytest;

import concurrencytest.checkpoint.CheckpointImpl;

import java.util.Objects;

public class ThreadState {

    private final String actorIdentification;
    private final CheckpointImpl checkpoint;
    private final boolean alive;
    private final boolean runnable;
    private final String details;

    // TODO loop count

    public ThreadState(String actorIdentification, CheckpointImpl checkpoint, boolean alive, boolean runnable, String details, int loopcount) {
        this.actorIdentification = actorIdentification;
        this.checkpoint = checkpoint;
        this.alive = alive;
        this.runnable = runnable;
        this.details = details;
    }

    public ThreadState(String actorIdentification, CheckpointImpl checkpoint, boolean alive, boolean runnable, int loopcount) {
        this(actorIdentification, checkpoint, alive, runnable, "", loopcount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadState that = (ThreadState) o;
        return alive == that.alive && runnable == that.runnable && Objects.equals(actorIdentification, that.actorIdentification) && Objects.equals(checkpoint, that.checkpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorIdentification, checkpoint, alive, runnable);
    }

    public String getActorIdentification() {
        return actorIdentification;
    }

    public CheckpointImpl getCheckpoint() {
        return checkpoint;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isRunnable() {
        return runnable;
    }

    @Override
    public String toString() {
        return "ThreadState{" +
                "actorIdentification='" + actorIdentification + '\'' +
                ", checkpoint=" + checkpoint +
                ", alive=" + alive +
                ", runnable=" + runnable +
                ", details=" + details +
                '}';
    }
}
