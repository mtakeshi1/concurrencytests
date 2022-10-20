package concurrencytest.v2.test;

import concurrencytest.exception.DeadlockFoundException;
import concurrencytest.runner.ActorSchedulingException;
import concurrencytest.runner.InitialPathBlockedException;
import org.junit.Test;

import java.util.List;

public class DeadlockCheck extends AbstractRunnerTests {


    @Test
    public void testEnsureDeadLock() {
        runExpectError(DeadlockTest.class, List.of("actor1", "actor1", "actor2"), e -> e instanceof DeadlockFoundException);
    }

    @Test
    public void testInitialPathBlocked() {
        runExpectError(DeadlockTest.class, List.of("actor1", "actor1", "actor2", "actor2"), e -> e instanceof InitialPathBlockedException);
    }


}
