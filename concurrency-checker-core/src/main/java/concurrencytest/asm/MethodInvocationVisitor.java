package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.MethodInvocationMatcher;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Member;

public class MethodInvocationVisitor extends BaseClassVisitor {

    private final MethodInvocationMatcher matcher;

    public MethodInvocationVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver, MethodInvocationMatcher matcher) {
        super(delegate, register, classUnderEnhancement, classResolver);
        this.matcher = matcher;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodInvocationVisistor(delegate, access, name, descriptor);
    }

    private class MethodInvocationVisistor extends BaseMethodVisitor {
        public MethodInvocationVisistor(MethodVisitor delegate, int access, String name, String descriptor) {
            super(classUnderEnhancement, delegate, MethodInvocationVisitor.this.checkpointRegister, MethodInvocationVisitor.this.sourceName, access, name, descriptor, MethodInvocationVisitor.this.classResolver);
        }

        //TODO check how the last parameter (isInterface) interacts with interfaces and default methods (and maybe records?)
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            Class<?> callTarget = resolveType(null, owner.replace('/', '.'));
            Member method = resolveMethodOrConstructor(callTarget, name, descriptor);
            if (matcher.matches(classUnderEnhancement, callTarget, name, descriptor, method.getModifiers(), opcode, InjectionPoint.BEFORE)) {
                injectCheckpoint(method, InjectionPoint.BEFORE);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (matcher.matches(classUnderEnhancement, callTarget, name, descriptor, method.getModifiers(), opcode, InjectionPoint.AFTER)) {
                injectCheckpoint(method, InjectionPoint.AFTER);
            }
        }

        private void injectCheckpoint(Member method, InjectionPoint before) {
            Checkpoint checkpoint = checkpointRegister.newMethodCheckpoint(sourceName, latestLineNumber, method, before);
            super.invokeEmptyCheckpoint(checkpoint);
        }
    }

}
