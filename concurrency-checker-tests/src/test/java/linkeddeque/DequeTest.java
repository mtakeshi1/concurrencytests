package linkeddeque;

import concurrencytest.annotations.*;
import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;
import concurrencytest.asm.ManualCheckpointVisitor;
import concurrencytest.config.*;
import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import linkeddeque.CopyConcurrentLinkedDeque.CopyNode;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Test to check if we can detect this bug
 *
 * https://bugs.openjdk.org/browse/JDK-8256833
 */
@RunWith(ActorSchedulerRunner.class)
public class DequeTest {

    private final CopyConcurrentLinkedDeque<Integer> queue = new CopyConcurrentLinkedDeque<>();

    private int actor1Result;
    private int actor2Result;

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
        CheckpointRuntimeAccessor.manualCheckpoint();
        actor2Result = queue.peekLast();
    }

    @AfterActorsCompleted
    public void observations() {
        if(actor1Result == 1) {
            Assert.assertEquals(2, actor2Result);
        } else if(actor1Result == 2) {
            Assert.assertEquals(1, actor2Result);
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
