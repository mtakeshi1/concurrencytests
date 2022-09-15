package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.util.ClassResolver;
import org.objectweb.asm.*;

import java.util.Collection;

public class SynchronizedMethodDeclarationVisitor extends BaseClassVisitor {

    public SynchronizedMethodDeclarationVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Collection<BehaviourModifier> mods = BehaviourModifier.unreflect(access);
        //TODO maybe define a new method, wrap try...finally around it and sync on it
        if (!mods.contains(BehaviourModifier.SYNCHRONIZED)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        int newMods = 0;
        for (var mod : mods) {
            if (mod != BehaviourModifier.SYNCHRONIZED) {
                newMods |= mod.modifier();
            }
        }
        String newName = generateDelegateMethodName(name);
        createDelegator(name, descriptor, signature, exceptions, mods, newMods, newName, AccessModifier.unreflect(access));

        newMods |= BehaviourModifier.SYNTHETIC.modifier() | BehaviourModifier.FINAL.modifier() | AccessModifier.PRIVATE.modifier();
        return super.visitMethod(newMods, newName, descriptor, signature, exceptions);
    }

    private void createDelegator(String name, String descriptor, String signature, String[] exceptions, Collection<BehaviourModifier> mods, int newMods, String newName, AccessModifier accessModifier) {
        MethodVisitor originalMethod = super.visitMethod(newMods | accessModifier.modifier(), name, descriptor, signature, exceptions);
        originalMethod.visitCode();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label handler = new Label();
        originalMethod.visitTryCatchBlock(tryStart, tryEnd, handler, null);
        pushMonitorOwner(mods, originalMethod);
        originalMethod.visitInsn(Opcodes.MONITORENTER);
        originalMethod.visitLabel(tryStart);
        int opcode;
        int nextArg = 0;
        if (!mods.contains(BehaviourModifier.STATIC)) {
            originalMethod.visitVarInsn(Opcodes.ALOAD, 0);
            opcode = Opcodes.INVOKEVIRTUAL;
            nextArg++;
        } else {
            opcode = Opcodes.INVOKESTATIC;
        }
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        for(Type arg : argumentTypes) {
            originalMethod.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), nextArg);
            nextArg += arg.getSize();
        }
        originalMethod.visitMethodInsn(opcode, Type.getInternalName(classUnderEnhancement), newName, descriptor, false);
        pushMonitorOwner(mods, originalMethod);
        originalMethod.visitInsn(Opcodes.MONITOREXIT);
        originalMethod.visitLabel(tryEnd);
        Type returnType = Type.getReturnType(descriptor);
        if(returnType != Type.VOID_TYPE) {
           originalMethod.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
        } else {
            originalMethod.visitInsn(Opcodes.RETURN);
        }
        originalMethod.visitLabel(handler);
        pushMonitorOwner(mods, originalMethod);
        originalMethod.visitInsn(Opcodes.MONITOREXIT);
        originalMethod.visitInsn(Opcodes.ATHROW);
        originalMethod.visitMaxs(0, 0);
        originalMethod.visitEnd();
    }

    private void pushMonitorOwner(Collection<BehaviourModifier> mods, MethodVisitor originalMethod) {
        if (!mods.contains(BehaviourModifier.STATIC)) {
            originalMethod.visitVarInsn(Opcodes.ALOAD, 0);
        } else {
            originalMethod.visitLdcInsn(classUnderEnhancement);
        }
    }

    public static String generateDelegateMethodName(String name) {
        return name + "$$delegate$$";
    }
}
