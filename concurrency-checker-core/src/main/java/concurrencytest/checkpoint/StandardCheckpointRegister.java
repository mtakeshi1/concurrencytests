package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.*;
import concurrencytest.checkpoint.instance.ParkCheckpoint;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StandardCheckpointRegister implements CheckpointRegister, Serializable {

    private final Map<CheckpointDescription, Checkpoint> allCheckpoints = new LinkedHashMap<>();

    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private final Checkpoint taskStartCheckpoint;

    private final Checkpoint taskFinishedCheckpoint;// = new Checkpoint(idGenerator.incrementAndGet(), new FixedCheckpoint(InjectionPoint.AFTER, "ACTOR_FINISHING"));

    @Override
    public Checkpoint newFieldCheckpoint(InjectionPoint injectionPoint, Type declaringClass, String fieldName, Type fieldType, boolean read, String details, String sourceFile, int lineNumber) {
        return registerCheckpoint(new FieldAccessCheckpointImpl(injectionPoint, details, sourceFile, lineNumber, declaringClass.getClassName(), fieldName, fieldType.getClassName(), read));
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
    public Checkpoint newMonitorEnterCheckpoint(Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        return registerCheckpoint(new MonitorCheckpointDescriptionImpl(injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, true));
    }

    @Override
    public Checkpoint newMonitorExitCheckpoint(Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint) {
        return registerCheckpoint(new MonitorCheckpointDescriptionImpl(injectionPoint, monitorOwnerType.getClassName(), sourceName, latestLineNumber, false));
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
        return registerCheckpoint(new ArrayElementCheckpointDescription(injectionPoint, "", sourceName, latestLineNumber, Object.class.getName(), arrayRead));
    }

    @Override
    public Checkpoint managedThreadStartedCheckpoint(String classUnderEnhancementName, String methodName, String methodDescriptor, String sourceName, int latestLineNumber) {
        return registerCheckpoint(new ThreadStartingCheckpoint(classUnderEnhancementName, methodName, sourceName, latestLineNumber));
    }

    @Override
    public Checkpoint newLockAcquireCheckpoint(InjectionPoint injectionPoint, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, String sourceName, int latestLineNumber) {
        return this.registerCheckpoint(
                new LockAcquireCheckpointDescription(injectionPoint, "", sourceName, latestLineNumber, methodName.equals("tryLock"), methodName.equals("tryLock") && Type.getArgumentTypes(methodDescriptor).length > 0)
        );
    }

    @Override
    public Checkpoint newLockReleasedCheckpoint(InjectionPoint injectionPoint, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, String sourceName, int latestLineNumber) {
        return this.registerCheckpoint(new LockReleaseCheckpointDescription(injectionPoint, "", sourceName, latestLineNumber));
    }

    @Override
    public Checkpoint newObjectWaitCheckpoint(String sourceName, int latestLineNumber, boolean monitorWait, boolean timedWait, InjectionPoint injectionPoint) {
        return this.registerCheckpoint(new WaitAwaitCheckpointDescription(sourceName, latestLineNumber, monitorWait, timedWait, injectionPoint));
    }

    @Override
    public Checkpoint newNotifyCheckpoint(boolean notifyAll, String sourceName, int latestLineNumber, boolean monitorNotify) {
        return this.registerCheckpoint(new NotifySignalCheckpointDescription(sourceName, latestLineNumber, monitorNotify, notifyAll));
    }
}
