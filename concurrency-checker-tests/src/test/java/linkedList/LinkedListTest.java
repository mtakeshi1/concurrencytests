package linkedList;

import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.v2.AfterActorsCompleted;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import linkedList.NonBlockingLinkedList.Node;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

@RunWith(ActorSchedulerRunner.class)
public class LinkedListTest {

    private final NonBlockingLinkedList<Integer> list = new NonBlockingLinkedList<>();

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(LinkedListTest.class) {
            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(NonBlockingLinkedList.class, Node.class);
            }

            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public int parallelExecutions() {
                return 12;
            }
        };
    }

    @MultipleActors(numberOfActors = 2)
    public void actor(int index) {
        list.prepend(index);
        Assert.assertNotNull(list.removeFirst());
    }

    @AfterActorsCompleted
    public void listEmpty() {
        Assert.assertEquals(0, list.size());
    }


}
