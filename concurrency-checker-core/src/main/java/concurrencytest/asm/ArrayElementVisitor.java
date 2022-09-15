package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.util.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ArrayElementVisitor extends BaseClassVisitor {

    private final ArrayElementMatcher matcher;

    public ArrayElementVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver, ArrayElementMatcher arrayElementMatcher) {
        super(delegate, register, classUnderEnhancement, classResolver);
        this.matcher = arrayElementMatcher;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new BaseMethodVisitor(classUnderEnhancement, delegate, ArrayElementVisitor.this.checkpointRegister, sourceName, access, name, descriptor, classResolver) {

            @Override
            public void visitInsn(int opcode) {
                Class<?> arrayType = Object[].class; // no support yet
                if (opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD && matcher.injectCheckpoint(classUnderEnhancement, super.resolvedMethod(), InjectionPoint.BEFORE, true)) {
                    injectCheckpoint(InjectionPoint.BEFORE, true, arrayType);
                } else if (opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE && matcher.injectCheckpoint(classUnderEnhancement, super.resolvedMethod(), InjectionPoint.BEFORE, false)) {
                    injectCheckpoint(InjectionPoint.BEFORE, false, arrayType);
                }
                super.visitInsn(opcode);
                if (opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD && matcher.injectCheckpoint(classUnderEnhancement, super.resolvedMethod(), InjectionPoint.AFTER, true)) {
                    injectCheckpoint(InjectionPoint.AFTER, true, arrayType);
                } else if (opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE && matcher.injectCheckpoint(classUnderEnhancement, super.resolvedMethod(), InjectionPoint.AFTER, false)) {
                    injectCheckpoint(InjectionPoint.AFTER, false, arrayType);
                }
            }

            private void injectCheckpoint(InjectionPoint injectionPoint, boolean arrayRead, Class<?> arrayType) {
                invokeEmptyCheckpoint(checkpointRegister.arrayElementCheckpoint(injectionPoint, arrayRead, arrayType, sourceName, latestLineNumber));
            }
        };
    }
}
