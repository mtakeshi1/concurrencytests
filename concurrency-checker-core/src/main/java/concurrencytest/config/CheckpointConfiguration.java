package concurrencytest.config;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.asm.BehaviourModifier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Controls where the checkpoints will be injected
 * <p>
 * TODO this should really be on a per class basis.
 */
public interface CheckpointConfiguration {

    /**
     * Injection points for method calss
     */
    default Collection<MethodInvocationMatcher> methodsToInstrument() {
        return Collections.emptyList();
    }

    /**
     * Injection points for field read / write
     */
    default Collection<FieldAccessMatch> fieldsToInstrument() {
        return List.of(
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> isFieldRead && injectionPoint == InjectionPoint.AFTER && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL),
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> !isFieldRead && injectionPoint == InjectionPoint.BEFORE && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL)
        );
    }

    /**
     * Injection points for array elements operations
     */
    default Collection<ArrayElementMatcher> arrayCheckpoints() {
        return List.of((classUnderEnhancement, method, injectionPoint, arrayElementRead) -> true);
    }

    /**
     * Inject default checkpoints. See {@link concurrencytest.asm.utils.SpecialMethods}
     */
    default boolean includeStandardMethods() {
        return true;
    }

    /**
     * Whether wait / park etc should be 'enhanced'. If true, these methods will be removed and replaced by special constructs
     */
    default boolean enhanceWaitParkNotify() {
        return true;
    }

    /**
     * If <code>true</code>, threads created within actor methods also becomes actors themselves
     * As of now, this only works for standard {@link Thread} (and not subclasses)
     */
    default boolean threadsEnhanced() {
        return true;
    }

    /**
     * If <code>false</code>, manual checkpoints get replaced by no-ops
     */
    default boolean manualCheckpointsEnabled() {
        return true;
    }

    /**
     * If <code>true</code> monitor enter / exit gets special treatment and the runtime will track them for deadlocks.
     */
    default boolean monitorCheckpointEnabled() {
        return true;
    }

    /**
     * If <code>true</code>, methods marked with 'sychronized' gets replaced by synchronized blocks.
     */
    default boolean removeSynchronizedMethodDeclaration() {
        return true;
    }

    /**
     * If <code>true</code>, {@link Lock#lock()} and friends are also tracked for deadlocks.
     */
    default boolean lockAcquisitionCheckpointEnabled() {
        return true;
    }

}
