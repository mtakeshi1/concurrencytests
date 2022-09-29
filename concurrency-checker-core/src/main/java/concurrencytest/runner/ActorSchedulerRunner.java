package concurrencytest.runner;

import concurrencytest.annotations.Actor;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
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
            setup.initialize();
        } catch (IOException e) {
            notifier.fireTestFailure(new Failure(childDescription(), e.getCause()));
        }
    }

    protected Configuration parseConfiguration() throws IOException {
        //TODO implement properly
        String folderPref = testClass.getName().substring(testClass.getName().lastIndexOf('.') + 1);
        return new BasicConfiguration(List.of(testClass), testClass, Files.createTempDirectory(folderPref).toFile());
    }
}
