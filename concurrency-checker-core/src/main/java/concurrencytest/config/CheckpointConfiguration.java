package concurrencytest.config;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.ArrayElementMatcher;
import concurrencytest.asm.BehaviourModifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface CheckpointConfiguration {

    default Collection<MethodInvocationMatcher> methodsToInstrument() {
        return Collections.emptyList();
    }

    default Collection<FieldAccessMatch> fieldsToInstrument() {
        return List.of(
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> isFieldRead && injectionPoint == InjectionPoint.AFTER && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL),
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> !isFieldRead && injectionPoint == InjectionPoint.BEFORE && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL)
        );
    }

    default Collection<ArrayElementMatcher> arrayCheckpoints() {
        return List.of((classUnderEnhancement, method, injectionPoint, arrayElementRead) -> true);
    }

    default boolean enhanceWaitParkNotify() {
        return true;
    }

    default boolean threadsEnhanced() {
        return true;
    }

    default boolean manualCheckpointsEnabled() {
        return true;
    }

    default boolean monitorCheckpointEnabled() {
        return true;
    }

    default boolean removeSynchronizedMethodDeclaration() {
        return true;
    }


}
