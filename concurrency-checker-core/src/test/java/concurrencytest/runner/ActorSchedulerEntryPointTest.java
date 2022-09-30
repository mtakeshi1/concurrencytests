package concurrencytest.runner;

import concurrencytest.runtime.tree.HeapTree;
import org.junit.Test;

import java.util.ArrayDeque;

public class ActorSchedulerEntryPointTest {

    @Test
    public void testScheduler() throws Exception {
        ActorSchedulerEntryPoint.selectNextActor(
                "", new HeapTree().rootNode(), null, new ArrayDeque<>(), 50
        );
    }

}