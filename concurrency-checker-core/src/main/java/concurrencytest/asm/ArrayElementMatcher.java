package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;

import java.lang.reflect.Method;

public interface ArrayElementMatcher {

    boolean injectCheckpoint(Class<?> classUnderEnhancement, Method method, /* Class<?> arrayType,*/ InjectionPoint injectionPoint, boolean arrayElementRead);

}
