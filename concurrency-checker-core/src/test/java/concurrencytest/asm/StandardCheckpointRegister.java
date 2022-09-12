package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.*;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StandardCheckpointRegister implements CheckpointRegister {

    private final Map<Integer, Checkpoint> allCheckpoints = new HashMap<>();

    private final AtomicInteger idGenerator = new AtomicInteger();

    @Override
    public FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber) {

        FieldAccessCheckpointImpl fieldAccessCheckpoint = new FieldAccessCheckpointImpl(
                idGenerator.incrementAndGet(), injectionPoint, details, sourceFile, lineNumber, declaringClass, fieldType, fieldName, read
        );
        allCheckpoints.put(fieldAccessCheckpoint.checkpointId(), fieldAccessCheckpoint);
        return fieldAccessCheckpoint;
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
}
