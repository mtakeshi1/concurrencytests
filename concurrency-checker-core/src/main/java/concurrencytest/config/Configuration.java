package concurrencytest.config;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.BehaviourModifier;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface Configuration {

    default ExecutionMode executionMode() {
        return ExecutionMode.AUTO;
    }

    default int parallelExecutions() {
        return 1;
    }

    default boolean removeSynchronizedMethodDeclaration() {
        return true;
    }

    default Collection<MethodInvocationMatcher> methodsToInstrument() {
        return Collections.emptyList();
    }

    default Collection<FieldAccessMatch> fieldsToInstrument() {
        return List.of(
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> isFieldRead && injectionPoint == InjectionPoint.AFTER && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL),
                (classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, behaviourModifiers, isFieldRead, injectionPoint) -> !isFieldRead && injectionPoint == InjectionPoint.BEFORE && behaviourModifiers.contains(BehaviourModifier.VOLATILE) && !behaviourModifiers.contains(BehaviourModifier.FINAL)
        );
    }

    default boolean checkpointAfterMonitorAcquire() {
        return true;
    }

    default boolean checkpointBeforeMonitorRelease() {
        return true;
    }

    default boolean offHeapTree() {
        return false;
    }

    default int maxLoopIterations() {
        return 100;
    }

    default Duration maxDurationPerRun() {
        return Duration.ofMinutes(10);
    }

    default Duration maxTotalDuration() {
        return Duration.ofHours(1);
    }

    default boolean randomExploration() {
        return false;
    }

    Collection<Class<?>> classesToInstrument();


}
