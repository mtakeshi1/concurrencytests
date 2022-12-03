package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class WaitParkWakeupVisitor extends BaseClassVisitor {

    public WaitParkWakeupVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new BaseMethodVisitor(classUnderEnhancement, delegate, checkpointRegister, sourceName, access, name, descriptor, classResolver) {

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                String callTarget = Type.getObjectType(owner).getClassName();
                if (isWait(name, descriptor)) {
                    // monitor_target, args
                    dropArguments(descriptor);
                    // monitor_target
                    super.visitInsn(Opcodes.DUP);
                    // monitor_target, monitor_target
                    super.visitInsn(Opcodes.MONITOREXIT);
                    // monitor_target
                    super.visitInsn(Opcodes.DUP);
                    // monitor_target, monitor_target
                    invokeGenericCheckpointWithContext(checkpointRegister.newObjectWaitCheckpoint(sourceName, latestLineNumber));
                    // monitor_target
                    super.visitInsn(Opcodes.MONITORENTER);
                    // empty stack
                } else if (isPark(owner, name)) {
                    dropArguments(descriptor);
                    super.visitInsn(Opcodes.POP);
                    // top of the stack should be the wait target. We unlock maybe?
                    invokeEmptyCheckpoint(checkpointRegister.newParkCheckpoint(callTarget + "." + name, sourceName, latestLineNumber));
                } else if (isNotify(owner, name, descriptor)) {
                    // monitor_target
                    var cp = checkpointRegister.newNotifyCheckpoint(name.equals("notifyAll"), sourceName, latestLineNumber);
                    super.visitInsn(Opcodes.DUP);
                    invokeGenericCheckpointWithContext(cp);
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }

            private void dropArguments(String descriptor) {
                for (Type arg : Type.getArgumentTypes(descriptor)) {
                    if (arg.getSize() == 2) {
                        super.visitInsn(Opcodes.POP2);
                    } else {
                        super.visitInsn(Opcodes.POP);
                    }
                }
            }

            private boolean isNotify(String owner, String name, String descriptor) {
                //                } else if (("jdk/internal/misc/Unsafe".equals(owner) || "sun/misc/Unsafe".equals(owner)) && "unpark".equals(name)) {
                //                    return true;
                //                } else if ("java/util/concurrent/locks/LockSupport".equals(owner)) {
                //                    return name.startsWith("unpark");
                return ("notify".equals(name) || "notifyAll".equals(name)) && Type.getArgumentTypes(descriptor).length == 0;
            }

            private boolean isWait(String name, String descriptor) {
                if ("wait".equals(name)) {
                    Type[] argumentTypes = Type.getArgumentTypes(descriptor);
                    return argumentTypes.length == 0 ||
                            (argumentTypes.length == 1 && argumentTypes[0].equals(Type.LONG_TYPE)) ||
                            (argumentTypes.length == 2 && argumentTypes[0].equals(Type.LONG_TYPE) && argumentTypes[1].equals(Type.INT_TYPE));
                }
                return false;
            }

            private boolean isPark(String owner, String name) {
                if (("jdk/internal/misc/Unsafe".equals(owner) || "sun/misc/Unsafe".equals(owner)) && "park".equals(name)) {
                    return true;
                } else if ("java/util/concurrent/locks/LockSupport".equals(owner)) {
                    return name.startsWith("park");
                }
                return false;
            }

        };
    }
}
