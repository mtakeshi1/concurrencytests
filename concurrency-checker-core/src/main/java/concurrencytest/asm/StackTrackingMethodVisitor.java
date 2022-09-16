package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.MethodVisitor;

public class StackTrackingMethodVisitor extends BaseMethodVisitor {

    //TODO implement this
    public StackTrackingMethodVisitor(Class<?> classUnderEnhancement, MethodVisitor delegate, CheckpointRegister register, String sourceName, int modifiers, String methodName, String descriptor, ClassResolver resolver) {
        super(classUnderEnhancement, delegate, register, sourceName, modifiers, methodName, descriptor, resolver);
    }
}
