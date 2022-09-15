package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.util.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class WaitParkWakupVisitor extends BaseClassVisitor {

    public WaitParkWakupVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new BaseMethodVisitor(classUnderEnhancement, delegate, checkpointRegister, sourceName, access, name, descriptor, classResolver) {

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                String callTarget = Type.getObjectType(owner).getClassName();
                if (isWaitOrPark(owner, name, descriptor)) {
                    dropArguments(descriptor);
                    invokeEmptyCheckpoint(checkpointRegister.newParkCheckpoint(callTarget + "." + name, sourceName, latestLineNumber));
                } else if (isNotifyOrUnpark(owner, name, descriptor)) {
                    invokeEmptyCheckpoint(checkpointRegister.newManualCheckpoint(callTarget + "." + name, sourceName, latestLineNumber));
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }

            private boolean isNotifyOrUnpark(String owner, String name, String descriptor) {
                if ("notify".equals(name) || "notifyAll".equals(name)) {
                    return true;
                } else if (("jdk/internal/misc/Unsafe".equals(owner) || "sun/misc/Unsafe".equals(owner)) && "unpark".equals(name)) {
                    return true;
                } else if ("java/util/concurrent/locks/LockSupport".equals(owner)) {
                    return name.startsWith("unpark");
                }
                return false;
            }

            private boolean isWaitOrPark(String owner, String name, String descriptor) {
                if ("wait".equals(name)) {
                    Type[] argumentTypes = Type.getArgumentTypes(descriptor);
                    return argumentTypes.length == 0 ||
                            (argumentTypes.length == 1 && argumentTypes[0].equals(Type.LONG_TYPE)) ||
                            (argumentTypes.length == 2 && argumentTypes[0].equals(Type.LONG_TYPE) && argumentTypes[1].equals(Type.INT_TYPE));
                } else if (("jdk/internal/misc/Unsafe".equals(owner) || "sun/misc/Unsafe".equals(owner)) && "park".equals(name)) {
                    return true;
                } else if ("java/util/concurrent/locks/LockSupport".equals(owner)) {
                    return name.startsWith("park");
                }
                return false;
            }

            private void dropArguments(String descriptor) {
                for (Type arg : Type.getArgumentTypes(descriptor)) {
                    if (arg.getSize() == 2) {
                        super.visitInsn(Opcodes.POP2);
                    } else {
                        super.visitInsn(Opcodes.POP);
                    }
                }
                if (!modifiers.contains(BehaviourModifier.STATIC)) {
                    super.visitInsn(Opcodes.POP);
                }
            }
        };
    }
}
