package concurrencytest.runner;

import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.DelegatingConfiguration;
import concurrencytest.runtime.tree.TreeNode;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ActorSchedulerRunner extends Runner {
    private final Class<?> testClass;
    private final String testName;
    private final Configuration configuration;

//    private Collection<? extends String> preselectedPath = Collections.emptyList();

    private volatile Consumer<TreeNode> treeObserver = ignored -> {
    };

    public ActorSchedulerRunner(Class<?> testClass) {
        this(testClass, new Class[0]);
    }

    public ActorSchedulerRunner(Class<?> testClass, Class<?>... additionalClasses) {
        this.testClass = testClass;
        this.testName = String.join("+", ActorSchedulerSetup.parseInitialActorNames(testClass));
        this.configuration = parseConfiguration(testClass, additionalClasses);
    }

    public ActorSchedulerRunner(Class<?> testClass, List<? extends String> preselectedPath, Class<?>... additionalClasses) {
        this.testClass = testClass;
        this.testName = String.join("+", ActorSchedulerSetup.parseInitialActorNames(testClass));
        this.configuration = new ConfigurationWithStartingPath(ActorSchedulerRunner.parseConfiguration(testClass, additionalClasses), preselectedPath);
    }

    @Override
    public Description getDescription() {
        Description suiteDescription = Description.createSuiteDescription(this.testClass);
        Description childDescription = childDescription();
        suiteDescription.addChild(childDescription);
        return suiteDescription;
    }

    private Description childDescription() {
        return Description.createTestDescription(testClass.getName(), testName, testClass.getAnnotations());
    }

    public void setTreeObserver(Consumer<TreeNode> treeObserver) {
        this.treeObserver = treeObserver;
    }

    @Override
    public void run(RunNotifier notifier) {
        Description description = childDescription();
        try {
            ActorSchedulerSetup setup = new ActorSchedulerSetup(configuration);
            notifier.fireTestStarted(description);
            Optional<Throwable> error = setup.run(treeObserver);
            error.ifPresent(t -> notifier.fireTestFailure(new Failure(description, t)));
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InstantiationException | ClassNotFoundException | RuntimeException |
                 ActorSchedulingException | TimeoutException e) {
            // infrastructure error =(
            notifier.fireTestFailure(new Failure(description, e));
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(description, e.getTargetException()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notifier.fireTestFailure(new Failure(description, e));
        } finally {
            notifier.fireTestFinished(description);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private static Configuration parseConfiguration(Class<?> testClass, Class<?>[] additionalClasses) {
        for (Method m : testClass.getMethods()) {
            if (m.isAnnotationPresent(ConfigurationSource.class) && Modifier.isStatic(m.getModifiers())) {
                try {
                    return (Configuration) m.invoke(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Configuration method: %s threw IllegalAccessException. Is it public?".formatted(m), e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Configuration method: %s threw %s - %s".formatted(m, e.getTargetException().getClass(), e.getTargetException().getMessage()), e.getTargetException());
                }
            }
        }
        return new BasicConfiguration(Arrays.asList(additionalClasses), testClass);
    }

    private static class ConfigurationWithStartingPath extends DelegatingConfiguration {
        private final List<? extends String> list;

        public ConfigurationWithStartingPath(Configuration delegate, List<? extends String> list) {
            super(delegate);
            this.list = list;
        }

        @Override
        public List<? extends String> startingPath() {
            return list;
        }
    }
}
