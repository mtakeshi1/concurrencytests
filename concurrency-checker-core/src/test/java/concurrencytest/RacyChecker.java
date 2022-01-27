package concurrencytest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import sut.RacyActorsFieldAccess;
import sut.RacyActorsGetters;
import sut.RacyIndyLambda;
import sut.ShouldDetectDeadlockTest;


public class RacyChecker extends RunListener {

    private volatile Throwable failure;

    @Test
    public void deadlockCheck() throws Exception {
        checkShouldFail(ShouldDetectDeadlockTest.class, "Should have detected deadlock", DeadlockFoundException.class);
    }

    @Test
    public void runField() throws Exception {
        checkShouldFail(RacyActorsFieldAccess.class, "Should have failed racing condition with field access", AssertionError.class);
    }

    @Test
    public void runMethod() throws Exception {
        checkShouldFail(RacyActorsGetters.class, "Should detect racing condition with method calls", AssertionError.class);
    }

    @Test
    public void testIndyLambda() throws Exception {
        checkShouldFail(RacyIndyLambda.class, "Should detect racing condition with indy to lambda", AssertionError.class);
    }

    private void checkShouldFail(Class<?> sut, String description, Class<?> throwableType) throws Exception {
        ConcurrencyRunner runner = new ConcurrencyRunner(sut);
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(this);
        runner.run(notifier);
        Assert.assertNotNull(description, failure);
        Assert.assertTrue(throwableType.isInstance(failure));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        this.failure = failure.getException();
        failure.getException().printStackTrace();
    }
}
