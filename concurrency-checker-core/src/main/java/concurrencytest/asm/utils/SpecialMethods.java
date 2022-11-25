package concurrencytest.asm.utils;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.config.MethodInvocationMatcher;

import java.lang.invoke.VarHandle;

/**
 * Some utility matcher for JDK methods marked as 'special' - that could / should have special treatment.
 */
public class SpecialMethods {

    private SpecialMethods() {
    }

    public static MethodInvocationMatcher THREAD_METHODS = (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
            Thread.class.equals(invocationTargetType) && injectionPoint == InjectionPoint.AFTER && methodName.equals("onSpinWait") || methodName.equals("join") || methodName.equals("interrupt") || methodName.equals("yield");

    public static MethodInvocationMatcher ATOMIC_METHODS = (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
            invocationTargetType.getName().startsWith("java.util.concurrent.atomic.");

    public static MethodInvocationMatcher VAR_HANDLE_METHODS = (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
            injectionPoint == InjectionPoint.AFTER && invocationTargetType.equals(VarHandle.class);

    public static MethodInvocationMatcher DEFAULT_SPECIAL_METHODS = (classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ->
            THREAD_METHODS.matches(classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ||
                    ATOMIC_METHODS.matches(classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint) ||
                    VAR_HANDLE_METHODS.matches(classUnderEnhancement, invocationTargetType, methodName, methodDescriptorType, accessModifier, behaviourModifiers, injectionPoint);
}
