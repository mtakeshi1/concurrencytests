package concurrencytest.asm;

import concurrencytest.CheckpointRuntimeAccessor;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ManualCheckpointVisitor extends ClassVisitor {
    private final CheckpointRegister register;

    private String source = "unknown";

    public ManualCheckpointVisitor(ClassVisitor classVisitor, CheckpointRegister register, Class<?> declaringClass) {
        super(Opcodes.ASM7, classVisitor);
        this.register = register;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.source = source;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ManualCheckpointMethodVisitor(delegate, register, source);
    }

    private class ManualCheckpointMethodVisitor extends BaseMethodVisitor {
        public ManualCheckpointMethodVisitor(MethodVisitor delegate, CheckpointRegister register, String source) {
            super(delegate, register, source);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKESTATIC && Type.getInternalName(CheckpointRuntimeAccessor.class).equals(owner) && "manualCheckpoint".equals(name)) {
                Checkpoint checkpoint = register.newManualCheckpoint(source, latestLineNumber);
                super.visitLdcInsn(checkpoint.checkpointId());
                if (Type.getMethodType(descriptor).getArgumentTypes().length > 0) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "checkpointReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.INT_TYPE), false);
                } else {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "checkpointReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }
}
