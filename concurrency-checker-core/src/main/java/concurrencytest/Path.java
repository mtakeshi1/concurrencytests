package concurrencytest;

import concurrencytest.checkpoint.OldCheckpointImpl;

import java.util.Map;
import java.util.Objects;

public class Path {
    private final String actorIdentification;
    private final OldCheckpointImpl checkpoint;
    private final Map<String, OldCheckpointImpl> state;

    public Path(String actorIdentification, OldCheckpointImpl checkpointId, Map<String, OldCheckpointImpl> state) {
        this.actorIdentification = actorIdentification;
        this.checkpoint = checkpointId;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Objects.equals(actorIdentification, path.actorIdentification) && Objects.equals(checkpoint, path.checkpoint) && Objects.equals(state, path.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorIdentification, checkpoint, state);
    }

    public String getActorIdentification() {
        return actorIdentification;
    }

    public OldCheckpointImpl getCheckpoint() {
        return checkpoint;
    }

    public Map<String, OldCheckpointImpl> getState() {
        return state;
    }
}
