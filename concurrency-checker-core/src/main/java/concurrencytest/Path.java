package concurrencytest;

import java.util.Map;
import java.util.Objects;

public class Path {
    private final String actorIdentification;
    private final Checkpoint checkpoint;
    private final Map<String, Checkpoint> state;

    public Path(String actorIdentification, Checkpoint checkpointId, Map<String, Checkpoint> state) {
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

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public Map<String, Checkpoint> getState() {
        return state;
    }
}
