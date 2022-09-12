package concurrencytest.config;

import concurrencytest.annotations.AccessModifier;
import concurrencytest.annotations.BehaviourModifier;
import concurrencytest.annotations.InjectionPoint;
import org.objectweb.asm.Type;

import java.util.List;

public interface MethodInvocationMatcher {


    default boolean matches(Class<?> classUnderEnhancement, Class<?> invocationTargetType, String methodName, String methodDescriptor, int modifiers, int opcode, InjectionPoint injectionPoint) {
        AccessModifier accessModifier = AccessModifier.unreflect(modifiers);
        List<BehaviourModifier> unreflect = BehaviourModifier.unreflect(modifiers);

        return matches(classUnderEnhancement, invocationTargetType, methodName, Type.getMethodType(methodDescriptor), accessModifier, unreflect, injectionPoint);
    }

    boolean matches(Class<?> classUnderEnhancement, Class<?> fieldOwner, String fieldName, Type methodDescriptorType,
                    AccessModifier accessModifier, List<BehaviourModifier> behaviourModifiers, InjectionPoint injectionPoint);


}
