package concurrencytest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import sut.ExceptionalRacingCondition;
import sut.RacyIndySynchronizedMethodRef;
import sut.SynchronizedActors;
import sut.SynchronizedValueHolderActors;

public class NonRacyChecker extends RunListener {

    private volatile Throwable failure;

    @Test
    public void testSynchronized() throws Exception {
        shouldNotFail(SynchronizedActors.class, "Synchronized actor methods should not fail");
    }

    @Test
    public void testSynchronizedMethod() throws Exception {
        shouldNotFail(SynchronizedValueHolderActors.class, "Should not detect racing condition on synchronized holders");
    }

    @Test
    @Ignore
    public void testSynchronizedIndy() throws Exception {
        shouldNotFail(RacyIndySynchronizedMethodRef.class, "Should not find racing condition on methodOrConstructor ref to a synchronized methodOrConstructor");
    }

    public void shouldNotFail(Class<?> sut, String message) throws Exception {
        ConcurrencyRunner runner = new ConcurrencyRunner(sut);
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(this);
        runner.run(notifier);
        Assert.assertNull(message, failure);
    }

    @Test
    public void testExceptionOnSyncBlock() throws Exception {
        shouldNotFail(ExceptionalRacingCondition.class, "Should not fail on exception thrown inside try { } catch");
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        this.failure = failure.getException();
        failure.getException().printStackTrace();
    }
}
