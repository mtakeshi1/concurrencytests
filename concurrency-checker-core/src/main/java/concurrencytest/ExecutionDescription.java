package concurrencytest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionDescription {

    private final Throwable error;

    private final List<Map.Entry<String, Checkpoint>> path;

    private ExecutionDescription(Throwable error, List<Map.Entry<String, Checkpoint>> path) {
        this.error = error;
        this.path = path;
    }

    private ExecutionDescription(List<Map.Entry<String, Checkpoint>> path) {
        this(null, path);
    }

    public static ExecutionDescription executionCompleted(List<Map.Entry<String, Checkpoint>> pathTaken) {
        return new ExecutionDescription(pathTaken);
    }

    public static ExecutionDescription deadlockFound(Collection<ManagedThread> alive, List<Map.Entry<String, Checkpoint>> pathTaken) {
//        throw new RuntimeException("TODO");
        //TODO add more information
        Collection<String> collect = alive.stream().map(ManagedThread::getName).collect(Collectors.toList());
        return new ExecutionDescription(new DeadlockFoundException(collect, "deadlock reached. Live threads that are (probably) locked: " + alive), pathTaken);
    }


    public boolean isFailed() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public List<Map.Entry<String, Checkpoint>> getPath() {
        return path;
    }

    public String createErrorDescription() {
        return path.stream().map(e -> e.getKey() + " -> " + e.getValue().getCheckpointName() + " ( " + e.getValue().getDescription() + " ) ").collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return "ExecutionDescription{" +
                "error=" + error +
                ", path=" + path +
                '}';
    }
}
