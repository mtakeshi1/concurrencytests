package concurrencytest.config;

import java.io.File;
import java.time.Duration;
import java.util.Collection;

public interface Configuration {

    default ExecutionMode executionMode() {
        return ExecutionMode.AUTO;
    }

    default boolean checkClassesBytecode() {
        return true;
    }

    default int parallelExecutions() {
        return 1;
    }

    default boolean offHeapTree() {
        return false;
    }

    default int maxLoopIterations() {
        return 100;
    }

    default Duration checkpointTimeout() {
        return Duration.ofMinutes(1);
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

    CheckpointConfiguration checkpointConfiguration();

    Collection<Class<?>> classesToInstrument();

    Class<?> mainTestClass();

    File outputFolder();

}
