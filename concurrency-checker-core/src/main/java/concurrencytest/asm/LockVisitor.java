package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.concurrent.locks.Lock;

public class LockVisitor extends BaseClassVisitor {

    public LockVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new BaseMethodVisitor(classUnderEnhancement, delegate, checkpointRegister, sourceName, access, name, descriptor, classResolver) {

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (owner.equals(Type.getInternalName(Lock.class))) {
                    switch (name) {
                        case "lock", "lockInterruptibly", "tryLock" -> beforeLockAcquire(opcode, owner, name, descriptor, isInterface);
                        case "unlock" -> afterLockReleased(opcode, owner, name, descriptor, isInterface);
                        case "newCondition" -> super.visitMethodInsn(opcode, owner, name, descriptor, isInterface); // TODO make a new checkpoint type for condition wait
                        default -> super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }

            private void afterLockReleased(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                Checkpoint checkpoint = checkpointRegister.newLockReleasedCheckpoint(InjectionPoint.AFTER, classUnderEnhancement, methodName, methodDescriptor, sourceName, latestLineNumber);
                invokeGenericCheckpointWithContext(checkpoint);
            }

            private void beforeLockAcquire(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                //stack should be Lock, params
                Type[] params = Type.getArgumentTypes(descriptor);
                int[] localVarsIndex = new int[params.length];
                for (int i = 0; i < params.length; i++) {
                    localVarsIndex[i] = nextFreeLocalVariable;
                    super.visitVarInsn(params[i].getOpcode(Opcodes.ISTORE), localVarsIndex[i]);
                }
                int lockLocalVar = nextFreeLocalVariable;
                super.visitVarInsn(Opcodes.ASTORE, nextFreeLocalVariable);
                //stack should be empty
                super.visitVarInsn(Opcodes.ALOAD, lockLocalVar);
                //stack should be Lock
                super.visitInsn(Opcodes.DUP);
                //stack should be Lock, Lock
                Checkpoint checkpoint = checkpointRegister.newLockAcquireCheckpoint(InjectionPoint.BEFORE, classUnderEnhancement, methodName, methodDescriptor, sourceName, latestLineNumber);
                invokeGenericCheckpointWithContext(checkpoint);
                //stack should be Lock
                for (int i = params.length - 1; i >= 0; i--) {
                    super.visitVarInsn(params[i].getOpcode(Opcodes.ILOAD), localVarsIndex[i]);
                }
                //stack should be Lock, params
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                Type returnType = Type.getReturnType(methodDescriptor);
                if (returnType == Type.BOOLEAN_TYPE) {
                    //stack should have a boolean
                    super.visitInsn(Opcodes.DUP);
                    //stack should have a boolean, boolean
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Boolean.class), "valueOf", Type.getMethodDescriptor(Type.getType(Boolean.class), Type.BOOLEAN_TYPE), false);
                    //stack should have a boolean, Boolean
                } else if (returnType == Type.VOID_TYPE) {
                    super.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class), "TRUE", Type.getDescriptor(Boolean.class));
                    //stack should have a Boolean
                } else {
                    throw new IllegalArgumentException("Did not expect return type %s from method: %s.%s".formatted(returnType, Lock.class.getName(), methodName));
                }
                checkpoint = checkpointRegister.newLockAcquireCheckpoint(InjectionPoint.AFTER, classUnderEnhancement, methodName, methodDescriptor, sourceName, latestLineNumber);
                invokeGenericCheckpointWithContext(checkpoint);
            }
        };
    }
}
