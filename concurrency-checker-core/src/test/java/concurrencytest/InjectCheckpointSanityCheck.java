package concurrencytest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import sut.GenerateCheckpointsTest;

public class InjectCheckpointSanityCheck extends RunListener {

    private volatile Throwable failure;

    public void shouldNotFail(Class<?> sut, String message) throws Exception {
        ConcurrencyRunner runner = new ConcurrencyRunner(sut);
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(this);
        runner.run(notifier);
        Assert.assertNull(message, failure);

    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        this.failure = failure.getException();
        failure.getException().printStackTrace();
    }

    @Test
    public void testMethodCheckpoint() throws Exception {
        shouldNotFail(GenerateCheckpointsTest.class,"should work");
    }
}
