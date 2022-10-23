package concurrencytest.runtime.tree;

import concurrencytest.runtime.lock.BlockCauseType;
import concurrencytest.runtime.lock.LockType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

public class ActorInformationTest {

    @Test
    public void testUnblocked() {
        var info = new BlockingCause(BlockCauseType.LOCK, Optional.of("actor"));
        var ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Optional.of(info), false);
        Assert.assertFalse(ai.isBlocked());
        info = new BlockingCause(BlockCauseType.MONITOR, Optional.of("actor"));
        ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Optional.of(info), false);
        Assert.assertFalse(ai.isBlocked());
    }

    @Test
    public void testBlocked() {
        var info = new BlockingCause(BlockCauseType.LOCK, Optional.of("actor2"));
        var ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Optional.of(info), false);
        Assert.assertTrue(ai.isBlocked());

        info = new BlockingCause(BlockCauseType.THREAD_JOIN, Optional.empty());
        ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Optional.of(info), false);
        Assert.assertTrue(ai.isBlocked());
    }

}