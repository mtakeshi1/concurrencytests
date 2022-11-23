package concurrencytest.config;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

public interface Configuration extends Serializable {

    default ExecutionMode executionMode() {
        return ExecutionMode.AUTO;
    }

    default boolean checkClassesBytecode() {
        return true;
    }

    default int parallelExecutions() {
        return 1;
    }

    default TreeMode treeMode() {
        return TreeMode.HEAP;
    }

    default int maxLoopIterations() {
        return 100;
    }

    default CheckpointDurationConfiguration durationConfiguration() {
        return new CheckpointDurationConfiguration();
    }

    default boolean traceCheckpoints() {
        return false;
    }

    default CheckpointConfiguration checkpointConfiguration() {
        return new CheckpointConfiguration() {
        };
    }

    Collection<Class<?>> classesToInstrument();

    Class<?> mainTestClass();

    File outputFolder();

}
