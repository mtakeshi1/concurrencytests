package concurrencytest.checkpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CheckpointImpl {

    private final long checkpointId;
    private final String checkpointName;
    private final String description;
    private final Map<String, Object> context;

    public CheckpointImpl(long checkpointId, String checkpointName, String description, Map<String, Object> state) {
        this.checkpointId = checkpointId;
        this.checkpointName = checkpointName;
        this.description = description;
        this.context = state;
    }

    public CheckpointImpl(long checkpointId, String checkpointName) {
        this(checkpointId, checkpointName, checkpointName, Collections.emptyMap());
    }

    public long checkpointId() {
        return checkpointId;
    }

    public String getCheckpointName() {
        return checkpointName;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckpointImpl that = (CheckpointImpl) o;
        return checkpointId == that.checkpointId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkpointId);
    }

    @Override
    public String toString() {
        return "CheckpointImpl{" +
                "checkpointId=" + checkpointId +
                ", checkpointName='" + checkpointName + '\'' +
                '}';
    }
}
