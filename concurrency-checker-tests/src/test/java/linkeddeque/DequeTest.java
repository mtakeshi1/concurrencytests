package linkeddeque;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.AfterActorsCompleted;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;
import concurrencytest.config.*;
import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import linkeddeque.CopyConcurrentLinkedDeque.CopyNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Test to check if we can detect this bug
 * <p>
 * https://bugs.openjdk.org/browse/JDK-8256833
 */
@RunWith(ActorSchedulerRunner.class)
public class DequeTest {

    private final CopyConcurrentLinkedDeque<Integer> queue = new CopyConcurrentLinkedDeque<>();

    private Integer actor1Result;
    private Integer actor2Result;

    @Before
    public void setup() {
        queue.addFirst(1);
    }

    @Actor
    public void actor1() {
        actor1Result = queue.pollFirst();
    }

    @Actor
    public void actor2() {
        queue.addFirst(2);
        CheckpointRuntimeAccessor.manualCheckpoint(
                CheckpointRuntimeAccessor.ignoringCheckpoints(() -> Helper.stringRep(queue))
        );
        actor2Result = queue.peekLast();
    }

    // none of them should see null
    @AfterActorsCompleted
    public void basicAssertions() {
        Assert.assertNotNull(actor1Result);
        Assert.assertNotNull(actor2Result);
    }

    @AfterActorsCompleted
    public void observations() {
        // in the beggining, the queue is [1]

        // if actor1 goes 'first', the queue becomes []

        // if actor2 goes 'first', the queue becomes [2, 1]
        // from this:
        //      - if actor1 polls first, the queue becomes [1], actor1 removed (2) and actor2 sees (1) as the only (and last) element
        //      - if actor2 peeks first, it should see (1) and actor1 should remove

        if (actor1Result == 1) {
            // actor1 removed (1) before actor2 inserted (2), so actor2 should only ever see (2) as a last element
            Assert.assertEquals("expected 1, 2 but was: 1, " + actor2Result, 2, actor2Result.intValue());
        } else if (actor1Result == 2) {
            // actor1 removed (2), so it happened after actor2 added (2). Actor2 should only be able to see (1)
            Assert.assertEquals("expected 2, 1 but was: 2, " + actor2Result, 1, actor2Result.intValue());
        }
    }

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(DequeTest.class) {
            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(CopyConcurrentLinkedDeque.class, CopyNode.class);
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
                                return invocationTargetType.equals(VarHandle.class) && injectionPoint == InjectionPoint.AFTER;
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

}
