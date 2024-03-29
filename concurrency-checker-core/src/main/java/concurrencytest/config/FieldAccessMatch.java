package concurrencytest.config;

import concurrencytest.asm.AccessModifier;
import concurrencytest.asm.BehaviourModifier;
import concurrencytest.annotations.InjectionPoint;
import org.objectweb.asm.Opcodes;

import java.util.Collection;

public interface FieldAccessMatch {

    default boolean matches(Class<?> classUnderEnhancement, Class<?> fieldOwner, String fieldName, Class<?> fieldType, int modifiers, int opcode, InjectionPoint injectionPoint) {
        AccessModifier accessModifier = AccessModifier.unreflect(modifiers);
        Collection<BehaviourModifier> unreflect = BehaviourModifier.unreflect(modifiers);

        return matches(classUnderEnhancement, fieldOwner, fieldName, fieldType, accessModifier, unreflect,
                opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC, injectionPoint);
    }

    boolean matches(Class<?> classUnderEnhancement, Class<?> fieldOwner, String fieldName, Class<?> fieldType, AccessModifier accessModifier, Collection<BehaviourModifier> behaviourModifiers, boolean isFieldRead, InjectionPoint injectionPoint);

}
