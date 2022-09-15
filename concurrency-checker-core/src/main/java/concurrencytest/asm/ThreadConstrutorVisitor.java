package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.ManagedThread;
import concurrencytest.util.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ThreadConstrutorVisitor extends BaseClassVisitor {

    public ThreadConstrutorVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ReplaceThreadConstructorVisitor(visitor, checkpointRegister, sourceName, access, name, descriptor);
    }

    private static class ReplaceThreadConstructorVisitor extends BaseMethodVisitor {
        public ReplaceThreadConstructorVisitor(MethodVisitor delegate, CheckpointRegister register, String sourceName, int modifiers, String methodName, String descriptor) {
            super(delegate, register, sourceName, modifiers, methodName, descriptor);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW && Type.getType(Thread.class).getInternalName().equals(type)) {
                super.visitTypeInsn(opcode, Type.getInternalName(ManagedThread.class));
            } else {
                super.visitTypeInsn(opcode, type);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (!isInterface && opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name) && Type.getType(Thread.class).getInternalName().equals(owner)) {
                super.visitMethodInsn(opcode, Type.getInternalName(ManagedThread.class), name, descriptor, false);
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }
}
