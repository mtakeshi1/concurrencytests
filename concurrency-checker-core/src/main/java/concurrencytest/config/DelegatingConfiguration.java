package concurrencytest.config;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class DelegatingConfiguration implements Configuration {

    private final Configuration delegate;

    public DelegatingConfiguration(Configuration delegate) {
        this.delegate = delegate;
    }

    public ExecutionMode executionMode() {
        return delegate.executionMode();
    }

    public boolean checkClassesBytecode() {
        return delegate.checkClassesBytecode();
    }

    public int parallelExecutions() {
        return delegate.parallelExecutions();
    }

    public TreeMode treeMode() {
        return delegate.treeMode();
    }

    public int maxLoopIterations() {
        return delegate.maxLoopIterations();
    }

    public CheckpointDurationConfiguration durationConfiguration() {
        return delegate.durationConfiguration();
    }

    public CheckpointConfiguration checkpointConfiguration() {
        return delegate.checkpointConfiguration();
    }

    public Collection<Class<?>> classesToInstrument() {
        return delegate.classesToInstrument();
    }

    public Class<?> mainTestClass() {
        return delegate.mainTestClass();
    }

    public File outputFolder() {
        return delegate.outputFolder();
    }

    public int maxSpuriousWakeups() {
        return delegate.maxSpuriousWakeups();
    }

    public List<? extends String> startingPath() {
        return delegate.startingPath();
    }
}
