package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.runtime.thread.ManagedThread;
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

    private class ReplaceThreadConstructorVisitor extends BaseMethodVisitor {
        public ReplaceThreadConstructorVisitor(MethodVisitor delegate, CheckpointRegister register, String sourceName, int modifiers, String methodName, String descriptor) {
            super(classUnderEnhancement, delegate, register, sourceName, modifiers, methodName, descriptor, ThreadConstrutorVisitor.this.classResolver);
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
            String threadInternalName = Type.getType(Thread.class).getInternalName();
            if (!isInterface && opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name) && threadInternalName.equals(owner)) {
                super.visitMethodInsn(opcode, Type.getInternalName(ManagedThread.class), name, descriptor, false);
            } else if (opcode == Opcodes.INVOKEVIRTUAL && name.equals("start") && (threadInternalName.equals(owner) || Type.getInternalName(ManagedThread.class).equals(owner))) {
                super.visitInsn(Opcodes.DUP);
                invokeGenericCheckpointWithContext(checkpointRegister.managedThreadStartedCheckpoint(classUnderEnhancement.getName(), methodName, methodDescriptor, sourceName, latestLineNumber));
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }
}
