package concurrencytest.runtime.tree;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

public class ActorInformationTest {

    @Test
    public void testUnblocked() {
        var info = new LockOrMonitorInformation(Object.class.getName(), Optional.empty(), "none", -1);
        var ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Collections.emptyList(), Optional.of(info), Optional.empty(), false);
        Assert.assertFalse(info.isBlocked("actor"));
        Assert.assertFalse(ai.isBlocked());
        info = new LockOrMonitorInformation(Object.class.getName(), Optional.of("actor"), "none", -1);
        ai = new ActorInformation("actor", 0, 0, Collections.emptyList(), Collections.emptyList(), Optional.of(info), Optional.empty(), false);
        Assert.assertFalse(ai.isBlocked());
    }

    @Test
    public void testBlocked() {
        var info = new LockOrMonitorInformation(Object.class.getName(), Optional.of("actor2"), "none", -1);
        var ai = new ActorInformation("actor1", 0, 0, Collections.emptyList(), Collections.emptyList(), Optional.of(info), Optional.empty(), false);
        Assert.assertFalse(info.isBlocked("actor2"));
        Assert.assertTrue(info.isBlocked("actor1"));
        Assert.assertTrue(ai.isBlocked());
    }

}