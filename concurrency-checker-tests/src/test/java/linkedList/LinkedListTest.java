package linkedList;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;
import concurrencytest.config.*;
import concurrencytest.runner.ActorSchedulerRunner;
import linkedList.NonBlockingLinkedList.Node;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
            public CheckpointDurationConfiguration durationConfiguration() {
                return new CheckpointDurationConfiguration(Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofHours(24));
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public boolean includeStandardMethods() {
                        return false;
                    }

                    @Override
                    public Collection<MethodInvocationMatcher> methodsCallsToInstrument() {
                        return List.of(new MethodInvocationMatcher() {
                            @Override
                            public boolean matches(Class<?> classUnderEnhancement, Class<?> invocationTargetType, String methodName, Type methodDescriptorType, AccessModifier accessModifier, Collection<BehaviourModifier> behaviourModifiers, InjectionPoint injectionPoint) {
//                                return methodName.equals("compareAndSet") && injectionPoint == InjectionPoint.BEFORE;
                                return invocationTargetType.equals(AtomicReference.class) && injectionPoint == InjectionPoint.AFTER;
                            }
                        });
                    }
                };
            }

            @Override
            public int parallelExecutions() {
                return 12;
            }

            @Override
            public TreeMode treeMode() {
                return TreeMode.COMPACT_HEAP;
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
