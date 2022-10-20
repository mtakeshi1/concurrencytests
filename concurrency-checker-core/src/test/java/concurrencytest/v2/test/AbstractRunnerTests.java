package concurrencytest.v2.test;

import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Subclasses of this class should test the end result of running test classes.
 */
public abstract class AbstractRunnerTests {

    private final List<File> files = new CopyOnWriteArrayList<>();

    @After
    public void cleaup() throws IOException {
        for (File outputFolder : files) {
            FileUtils.deltree(outputFolder);
        }
    }

    public void runExpectError(Class<?> mainTestClass, Predicate<Throwable> errorMatcher) {
        runExpectError(mainTestClass, Collections.emptyList(), errorMatcher);
    }

    public void runExpectError(Class<?> mainTestClass, Collection<? extends String> preselectedPath, Predicate<Throwable> errorMatcher) {
        ActorSchedulerRunner runner = new ActorSchedulerRunner(mainTestClass);
        runner.setPreselectedPath(preselectedPath);
        files.add(runner.getConfiguration().outputFolder());

        RunNotifier notifier = new RunNotifier();
        Throwable[] container = new Throwable[1];
        notifier.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) {
                container[0] = failure.getException();
            }
        });
        runner.run(notifier);
        Assert.assertNotNull(container[0]);
        container[0].printStackTrace();
        Assert.assertTrue(String.valueOf(container[0]), errorMatcher.test(container[0]));
    }

    public void runToCompletion(Class<?> mainTestClass) {
        runToCompletion(mainTestClass, ignored -> {
        });
    }

    public void runToCompletion(Class<?> mainTestClass, Consumer<TreeNode> treeObserver) {
        ActorSchedulerRunner runner = new ActorSchedulerRunner(mainTestClass);
        runner.setTreeObserver(treeObserver);
        files.add(runner.getConfiguration().outputFolder());
        RunNotifier notifier = new RunNotifier();
        Throwable[] container = new Throwable[1];
        notifier.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) {
                container[0] = failure.getException();
            }
        });
        runner.run(notifier);
        if (container[0] != null) {
            container[0].printStackTrace();
        }
        Assert.assertNull(container[0]);
    }
}
