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
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ActorSchedulerRunner extends Runner {
    private final Class<?> testClass;
    private final String testName;

    public ActorSchedulerRunner(Class<?> testClass) {
        this.testClass = testClass;
        this.testName = Arrays.stream(testClass.getMethods()).filter(m -> m.isAnnotationPresent(Actor.class)).map(Method::getName).collect(Collectors.joining("_"));
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
            Configuration configuration = parseConfiguration();
            ActorSchedulerSetup setup = new ActorSchedulerSetup(configuration);
            setup.run();
        } catch (IOException | IllegalAccessException | NoSuchMethodException | InstantiationException | ClassNotFoundException e) {
            // infrastructure error =(
            notifier.fireTestFailure(new Failure(childDescription(), e));
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(childDescription(), e.getTargetException()));
        } catch (ActorSchedulingException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notifier.fireTestFailure(new Failure(childDescription(), e));
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    protected Configuration parseConfiguration() throws IOException, InvocationTargetException, IllegalAccessException {
        for (Method m : testClass.getMethods()) {
            if (m.isAnnotationPresent(ConfigurationSource.class) && Modifier.isStatic(m.getModifiers())) {
                return (Configuration) m.invoke(null);
            }
        }
        String folderPref = testClass.getName().substring(testClass.getName().lastIndexOf('.') + 1);
        return new BasicConfiguration(testClass);
    }
}
