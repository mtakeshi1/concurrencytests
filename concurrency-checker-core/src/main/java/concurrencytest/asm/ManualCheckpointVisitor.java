package concurrencytest.asm;

import concurrencytest.runtime.CheckpointRuntimeAccessor;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ManualCheckpointVisitor extends BaseClassVisitor {

    public ManualCheckpointVisitor(ClassVisitor classVisitor, CheckpointRegister register, Class<?> declaringClass, ClassResolver classResolver) {
        super(classVisitor, register, declaringClass, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new ManualCheckpointMethodVisitor(delegate, checkpointRegister, sourceName, access, name, descriptor);
    }

    private class ManualCheckpointMethodVisitor extends BaseMethodVisitor {
        public ManualCheckpointMethodVisitor(MethodVisitor delegate, CheckpointRegister register, String source, int accessModifiers, String methodName, String methodDescriptor) {
            super(classUnderEnhancement, delegate, register, source, accessModifiers, methodName, methodDescriptor, ManualCheckpointVisitor.this.classResolver);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKESTATIC && Type.getInternalName(CheckpointRuntimeAccessor.class).equals(owner) && "manualCheckpoint".equals(name)) {
                Checkpoint checkpoint = checkpointRegister.newManualCheckpoint("", sourceName, latestLineNumber);
                if (Type.getMethodType(descriptor).getArgumentTypes().length > 0) {
                    super.invokeStringCheckpointWithContext(checkpoint);
                } else {
                    super.invokeEmptyCheckpoint(checkpoint);

                }
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }
}
