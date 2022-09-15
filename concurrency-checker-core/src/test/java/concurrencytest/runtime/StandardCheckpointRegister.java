package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.*;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StandardCheckpointRegister implements CheckpointRegister {

    private final Map<Integer, Checkpoint> allCheckpoints = new HashMap<>();

    private final AtomicInteger idGenerator = new AtomicInteger();

    private final Checkpoint taskStartCheckpoint = new FixedCheckpoint(idGenerator.incrementAndGet(), InjectionPoint.BEFORE, "ACTOR_STARTING");

    private final Checkpoint taskFinishedCheckpoint = new FixedCheckpoint(idGenerator.incrementAndGet(), InjectionPoint.AFTER, "ACTOR_FINISHING");

    @Override
    public FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber) {

        FieldAccessCheckpointImpl fieldAccessCheckpoint = new FieldAccessCheckpointImpl(
                idGenerator.incrementAndGet(), injectionPoint, details, sourceFile, lineNumber, declaringClass, fieldName, fieldType, read
        );
        allCheckpoints.put(fieldAccessCheckpoint.checkpointId(), fieldAccessCheckpoint);
        return fieldAccessCheckpoint;
    }

    public StandardCheckpointRegister() {
        allCheckpoints.put(taskStartCheckpoint.checkpointId(), taskStartCheckpoint);
        allCheckpoints.put(taskFinishedCheckpoint.checkpointId(), taskFinishedCheckpoint);
    }

    @Override
    public Checkpoint taskStartingCheckpoint() {
        return taskStartCheckpoint;
    }

    @Override
    public Checkpoint taskFinishedCheckpoint() {
        return taskFinishedCheckpoint;
    }

    @Override
    public Map<Integer, Checkpoint> allCheckpoints() {
        return allCheckpoints;
    }

    @Override
    public Checkpoint newManualCheckpoint(String details, String source, int latestLineNumber) {
        var checkpoint = new ManualCheckpointImpl(idGenerator.incrementAndGet(), details, source, latestLineNumber);
        allCheckpoints.put(checkpoint.checkpointId(), checkpoint);
        return checkpoint;
    }

    @Override
    public MonitorCheckpoint newMonitorEnterCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        var checkpoint = new MonitorCheckpointImpl(idGenerator.incrementAndGet(), injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, resolveType(monitorOwnerType), true);
        allCheckpoints.put(checkpoint.checkpointId(), checkpoint);
        return checkpoint;
    }

    private Class<?> resolveType(Type monitorOwnerType) {
        try {
            return ReflectionHelper.resolveType(monitorOwnerType.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MonitorCheckpoint newMonitorExitCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        var checkpoint = new MonitorCheckpointImpl(idGenerator.incrementAndGet(), injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, resolveType(monitorOwnerType), false);
        allCheckpoints.put(checkpoint.checkpointId(), checkpoint);
        return checkpoint;
    }

    @Override
    public Checkpoint newMethodCheckpoint(String sourceName, int latestLineNumber, Method method, InjectionPoint injectionPoint) {
        MethodCallCheckpointImpl checkpoint = new MethodCallCheckpointImpl(idGenerator.incrementAndGet(), injectionPoint, sourceName, latestLineNumber, method);
        allCheckpoints.put(checkpoint.checkpointId(), checkpoint);
        return checkpoint;
    }

    @Override
    public Checkpoint newParkCheckpoint(String details, String sourceName, int latestLineNumber) {
        //TODO
        return null;
    }
}
