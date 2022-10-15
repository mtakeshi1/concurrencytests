package concurrencytest.runner;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class ActorSchedulerRunner extends Runner {
    private final Class<?> testClass;
    private final String testName;
    private final Configuration configuration;

    public ActorSchedulerRunner(Class<?> testClass) {
        this.testClass = testClass;
        this.testName = Arrays.stream(testClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(Method::getName).collect(Collectors.joining("_"));
        this.configuration = parseConfiguration(testClass);
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


    @Override
    public void run(RunNotifier notifier) {
        try {
            ActorSchedulerSetup setup = new ActorSchedulerSetup(configuration);
            notifier.fireTestStarted(childDescription());
            Optional<Throwable> error = setup.run();
            error.ifPresent(t -> notifier.fireTestFailure(new Failure(childDescription(), t)));
            notifier.fireTestFinished(childDescription());
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InstantiationException | ClassNotFoundException | RuntimeException e) {
            // infrastructure error =(
            notifier.fireTestFailure(new Failure(childDescription(), e));
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(childDescription(), e.getTargetException()));
        } catch (ActorSchedulingException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notifier.fireTestFailure(new Failure(childDescription(), e));
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private static Configuration parseConfiguration(Class<?> testClass) {
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
        return new BasicConfiguration(testClass);
    }
}
