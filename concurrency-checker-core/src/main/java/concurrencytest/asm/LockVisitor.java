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
                invokeTryAcquireStaticConstructorSingleArg();
                invokeGenericCheckpointWithContext(checkpoint);
            }

            private void beforeLockAcquire(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                //stack should be Lock, params
                Type[] params = Type.getArgumentTypes(descriptor);
                int[] localVarsIndex = new int[params.length];
                for (int i = params.length - 1; i >= 0; i--) {
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
                invokeTryAcquireStaticConstructorSingleArg();
                invokeGenericCheckpointWithContext(checkpoint);
                //stack should be Lock
                for (int i = 0; i < params.length; i++) {
                    super.visitVarInsn(params[i].getOpcode(Opcodes.ILOAD), localVarsIndex[i]);
                }
                //stack should be Lock, params
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                // stack should be either empty or boolean
                Type returnType = Type.getReturnType(methodDescriptor);
                if (returnType == Type.BOOLEAN_TYPE) {
                    super.visitInsn(Opcodes.DUP);
                    super.visitVarInsn(Opcodes.ALOAD, lockLocalVar);
                    // stack should be boolean boolean Lock
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TryAcquireWithResult.class), "from", Type.getMethodDescriptor(Type.getType(TryAcquireWithResult.class), Type.BOOLEAN_TYPE, Type.getType(Lock.class)), false);
                    //stack should be boolean TryAcquireWithResult
                } else if (returnType == Type.VOID_TYPE) {
                    super.visitVarInsn(Opcodes.ALOAD, lockLocalVar);
                    invokeTryAcquireStaticConstructorSingleArg();
                    //stack should be TryAcquireWithResult
                } else {
                    throw new IllegalArgumentException("Did not expect return type %s from method: %s.%s".formatted(returnType, Lock.class.getName(), methodName));
                }

                // stack should be [boolean] TryAcquireWithResult
                checkpoint = checkpointRegister.newLockAcquireCheckpoint(InjectionPoint.AFTER, classUnderEnhancement, methodName, methodDescriptor, sourceName, latestLineNumber);
                invokeGenericCheckpointWithContext(checkpoint);
            }

            private void invokeTryAcquireStaticConstructorSingleArg() {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TryAcquireWithResult.class), "from", Type.getMethodDescriptor(Type.getType(TryAcquireWithResult.class), Type.getType(Lock.class)), false);
            }
        };
    }
}
