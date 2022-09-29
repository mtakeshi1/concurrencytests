package concurrencytest.config;

import java.io.File;
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

    default CheckpointDurationConfiguration durationConfiguration() {
        return new CheckpointDurationConfiguration();
    }

    default CheckpointConfiguration checkpointConfiguration() {
        return new CheckpointConfiguration() {
        };
    }

    Collection<Class<?>> classesToInstrument();

    Class<?> mainTestClass();

    File outputFolder();

}
