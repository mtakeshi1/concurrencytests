package concurrencytest.config;

import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;
import concurrencytest.annotations.InjectionPoint;
import org.objectweb.asm.Type;

import java.util.Collection;


public interface MethodInvocationMatcher {

    //they should use CheckpointLocation

    default boolean matches(Class<?> classUnderEnhancement, Class<?> invocationTargetType, String methodName, String methodDescriptor, int modifiers, int opcode, InjectionPoint injectionPoint) {
        AccessModifier accessModifier = AccessModifier.unreflect(modifiers);
        Collection<BehaviourModifier> unreflect = BehaviourModifier.unreflect(modifiers);

        return matches(classUnderEnhancement, invocationTargetType, methodName, Type.getMethodType(methodDescriptor), accessModifier, unreflect, injectionPoint);
    }

    boolean matches(Class<?> classUnderEnhancement, Class<?> invocationTargetType, String methodName, Type methodDescriptorType,
                    AccessModifier accessModifier, Collection<BehaviourModifier> behaviourModifiers, InjectionPoint injectionPoint);


}
