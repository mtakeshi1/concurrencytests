package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.*;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runtime.ParkCheckpoint;
import org.objectweb.asm.Type;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StandardCheckpointRegister implements CheckpointRegister {

    private final Map<CheckpointDescription, Checkpoint> allCheckpoints = new HashMap<>();

    private final AtomicInteger idGenerator = new AtomicInteger();

    private final Checkpoint taskStartCheckpoint;

    private final Checkpoint taskFinishedCheckpoint;// = new Checkpoint(idGenerator.incrementAndGet(), new FixedCheckpoint(InjectionPoint.AFTER, "ACTOR_FINISHING"));

    @Override
    public Checkpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber) {
        return registerCheckpoint(new FieldAccessCheckpointImpl(injectionPoint, details, sourceFile, lineNumber, declaringClass, fieldName, fieldType, read));
    }

    public StandardCheckpointRegister() {
        this.taskStartCheckpoint = registerCheckpoint(new FixedCheckpoint(InjectionPoint.BEFORE, "ACTOR_STARTING"));
        this.taskFinishedCheckpoint = registerCheckpoint(new FixedCheckpoint(InjectionPoint.AFTER, "ACTOR_FINISHING"));
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
    public Map<CheckpointDescription, Checkpoint> allCheckpoints() {
        return allCheckpoints;
    }

    @Override
    public Checkpoint newManualCheckpoint(String details, String source, int latestLineNumber) {
        return registerCheckpoint(new ManualCheckpointImpl(details, source, latestLineNumber));
    }

    private Checkpoint registerCheckpoint(CheckpointDescription description) {
        return allCheckpoints.computeIfAbsent(description, desc -> new Checkpoint(idGenerator.incrementAndGet(), desc));
    }

    @Override
    public Checkpoint newMonitorEnterCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        return registerCheckpoint(new MonitorCheckpointImpl(injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, resolveType(monitorOwnerType), true));
    }

    private Class<?> resolveType(Type monitorOwnerType) {
        try {
            return ReflectionHelper.resolveType(monitorOwnerType.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Checkpoint newMonitorExitCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        return registerCheckpoint(new MonitorCheckpointImpl(injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, resolveType(monitorOwnerType), false));
    }

    @Override
    public Checkpoint newMethodCheckpoint(String sourceName, int latestLineNumber, Member method, InjectionPoint injectionPoint) {
        return registerCheckpoint(new MethodCallCheckpointImpl(injectionPoint, sourceName, latestLineNumber, method));
    }

    @Override
    public Checkpoint newParkCheckpoint(String details, String sourceName, int latestLineNumber) {
        return registerCheckpoint(new ParkCheckpoint(details, sourceName, latestLineNumber));
    }

    @Override
    public Checkpoint arrayElementCheckpoint(InjectionPoint injectionPoint, boolean arrayRead, Class<?> arrayType, String sourceName, int latestLineNumber) {
        return registerCheckpoint(new ArrayElementCheckpointDescription(injectionPoint, "", sourceName, latestLineNumber, Object.class, arrayRead));
    }

    @Override
    public Checkpoint managedThreadStartedCheckpoint(String classUnderEnhancementName, String methodName, String methodDescriptor, String sourceName, int latestLineNumber) {
        return registerCheckpoint(new ThreadStartingCheckpoint(classUnderEnhancementName, methodName, sourceName, latestLineNumber));
    }
}
