package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class SynchronizedMethodDeclarationVisitor extends BaseClassVisitor {

    public SynchronizedMethodDeclarationVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, register, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Collection<BehaviourModifier> mods = BehaviourModifier.unreflect(access);
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
        int delegateMods = newMods;

        newMods |= BehaviourModifier.SYNTHETIC.modifier() | BehaviourModifier.FINAL.modifier() | AccessModifier.PRIVATE.modifier();
        MethodVisitor delegate = super.visitMethod(newMods, newName, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM9, delegate) {

            private final List<Consumer<MethodVisitor>> commands = new ArrayList<>();

            @Override
            public AnnotationVisitor visitAnnotationDefault() {
                AnnotationVisitor delegate = super.visitAnnotationDefault();
                RecordingAnnotationVisitor record = new RecordingAnnotationVisitor(delegate);
                commands.add(mv -> {
                    AnnotationVisitor delegateVisitor = mv.visitAnnotationDefault();
                    if (delegateVisitor != null) {
                        record.replay(delegateVisitor);
                    }
                });
                return record;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor originalDelegate = super.visitAnnotation(descriptor, visible);
                RecordingAnnotationVisitor record = new RecordingAnnotationVisitor(originalDelegate);
                commands.add(mv -> {
                    AnnotationVisitor delegateVisitor = mv.visitAnnotation(descriptor, visible);
                    if (delegateVisitor != null) {
                        record.replay(delegateVisitor);
                    }
                });
                return record;
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                AnnotationVisitor originalDelegate = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
                RecordingAnnotationVisitor record = new RecordingAnnotationVisitor(originalDelegate);
                commands.add(mv -> {
                    AnnotationVisitor delegateVisitor = mv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
                    if (delegateVisitor != null) {
                        record.replay(delegateVisitor);
                    }
                });
                return record;
            }

            @Override
            public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
                super.visitAnnotableParameterCount(parameterCount, visible);
                commands.add(mv -> mv.visitAnnotableParameterCount(parameterCount, visible));
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                AnnotationVisitor originalDelegate = super.visitParameterAnnotation(parameter, descriptor, visible);
                RecordingAnnotationVisitor record = new RecordingAnnotationVisitor(originalDelegate);
                commands.add(mv -> {
                    AnnotationVisitor delegateVisitor = mv.visitParameterAnnotation(parameter, descriptor, visible);
                    if (delegateVisitor != null) {
                        record.replay(delegateVisitor);
                    }
                });
                return record;
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                createDelegator(name, descriptor, signature, exceptions, mods, delegateMods, newName, AccessModifier.unreflect(access), commands);
            }
        };
    }

    private void createDelegator(String name, String descriptor, String signature, String[] exceptions, Collection<BehaviourModifier> mods, int newMods, String newName, AccessModifier accessModifier, List<Consumer<MethodVisitor>> commands) {
        MethodVisitor originalMethod = super.visitMethod(newMods | accessModifier.modifier(), name, descriptor, signature, exceptions);
        originalMethod.visitCode();
        for (var cmd : commands) {
            cmd.accept(originalMethod);
        }
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
        for (Type arg : argumentTypes) {
            originalMethod.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), nextArg);
            nextArg += arg.getSize();
        }
        originalMethod.visitMethodInsn(opcode, Type.getInternalName(classUnderEnhancement), newName, descriptor, false);
        pushMonitorOwner(mods, originalMethod);
        originalMethod.visitInsn(Opcodes.MONITOREXIT);
        originalMethod.visitLabel(tryEnd);
        Type returnType = Type.getReturnType(descriptor);
        if (returnType != Type.VOID_TYPE) {
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
