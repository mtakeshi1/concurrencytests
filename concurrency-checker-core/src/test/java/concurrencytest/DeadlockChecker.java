package concurrencytest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import sut.ExceptionalRacingCondition;
import sut.NestedSyncBlocks;
import sut.ShouldDetectDeadlockTest;

public class DeadlockChecker extends RunListener {

    private volatile Throwable failure;

    @Test
    public void run() throws Exception {
        ConcurrencyRunner runner = new ConcurrencyRunner(ShouldDetectDeadlockTest.class);
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(this);
        runner.run(notifier);
        Assert.assertNotNull("Should have failed", failure);
        Assert.assertTrue(failure instanceof DeadlockFoundException);
    }

    @Test
    @Ignore
    public void detectDeadlocksNestedSynchronized() {
        ConcurrencyRunner runner = new ConcurrencyRunner(NestedSyncBlocks.class);
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(this);
        runner.run(notifier);
        Assert.assertNotNull("Should have failed", failure);
        Assert.assertTrue(failure instanceof DeadlockFoundException);
    }



    @Override
    public void testFailure(Failure failure) throws Exception {
        this.failure = failure.getException();
    }
}
