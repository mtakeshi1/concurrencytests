package concurrencytest.config;

import concurrencytest.annotations.AccessModifier;
import concurrencytest.annotations.BehaviourModifier;
import concurrencytest.annotations.FieldCheckpoint;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.util.ArrayUtils;

import java.util.List;

public class FieldAnnotationMatch implements FieldAccessMatch {

    private final FieldCheckpoint fieldAnnotation;

    public FieldAnnotationMatch(FieldCheckpoint fieldAccessCheckpoint) {
        this.fieldAnnotation = fieldAccessCheckpoint;
    }

    @Override
    public boolean matches(Class<?> classUnderEnhancement, Class<?> fieldOwner, String fieldName,
                           Class<?> fieldType, AccessModifier accessModifier, List<BehaviourModifier> behaviourModifiers, boolean isFieldRead, InjectionPoint injectionPoint) {
        if ((fieldAnnotation.declaringClass() != Object.class && !fieldOwner.equals(fieldAnnotation.declaringClass())) || !fieldOwner.getName().matches(fieldAnnotation.declaringClassNameRegex())) {
            return false;
        }
        if (!fieldName.matches(fieldAnnotation.fieldNameRegex())) {
            return false;
        }
        if (!ArrayUtils.arrayContains(fieldAnnotation.injectionPoints(), injectionPoint)) {
            return false;
        }
        for (BehaviourModifier mod : behaviourModifiers) {
            if (!ArrayUtils.arrayContains(fieldAnnotation.behaviourModifiers(), mod)) {
                return false;
            }
        }
        if (isFieldRead && !fieldAnnotation.fieldRead()) {
            return false;
        }
        return isFieldRead || fieldAnnotation.fieldWrite();
    }
}
