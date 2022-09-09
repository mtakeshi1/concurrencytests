package concurrencytest.asm;

import concurrencytest.CheckpointRuntimeAccessor;
import concurrencytest.annotations.AccessModifier;
import concurrencytest.annotations.BehaviourModifier;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.FieldAccessCheckpoint;
import concurrencytest.config.FieldAccessMatch;
import concurrencytest.util.ClassResolver;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.Collection;

public class FieldCheckpointVisitor extends ClassVisitor {
    //TODO check if we need something like a classresolver
    private final FieldAccessMatch checkpointMatchConfiguration;
    private final CheckpointRegister checkpointRegister;
    private final Class<?> classUnderEnhancement;
    private final ClassResolver classResolver;

    private String source = "uknown";

    public FieldCheckpointVisitor(FieldAccessMatch fieldCheckpoint, ClassVisitor delegate, CheckpointRegister checkpointRegister, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(Opcodes.ASM7, delegate);
        this.checkpointMatchConfiguration = fieldCheckpoint;
        this.checkpointRegister = checkpointRegister;
        this.classUnderEnhancement = classUnderEnhancement;
        this.classResolver = classResolver;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new FieldCheckpointMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), access);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.source = source;
    }

    private class FieldCheckpointMethodVisitor extends BaseMethodVisitor {

        public FieldCheckpointMethodVisitor(MethodVisitor delegate, int access) {
            super(delegate, checkpointRegister, source, access);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String ownerClassName = owner.replace('/', '.');
            String fieldTypeName = Type.getType(descriptor).getClassName();
            Class<?> ownerType = classResolver.resolveName(ownerClassName);
            Field field = classResolver.lookupField(ownerType, name);
            Class<?> fieldType = classResolver.resolveName(fieldTypeName);
            if (FieldCheckpointVisitor.this.checkpointMatchConfiguration.matches(classUnderEnhancement,
                    ownerType, name, fieldType, field.getModifiers(), opcode, InjectionPoint.BEFORE)) {
                injectCheckpoint(opcode, ownerClassName, fieldTypeName, name, true);
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
            if (FieldCheckpointVisitor.this.checkpointMatchConfiguration.matches(classUnderEnhancement,
                    ownerType, name, classResolver.resolveName(fieldTypeName), field.getModifiers(), opcode, InjectionPoint.AFTER)) {
                injectCheckpoint(opcode, ownerClassName, fieldTypeName, name, false);
            }
        }

        private void injectCheckpoint(int opcode, String owner, String fieldClassName, String name, boolean before) {
            String details = String.format("%s %s %s.%s", before ? "BEFORE" : "AFTER", ReflectionHelper.renderOpcode(opcode), owner, name);
            FieldAccessCheckpoint checkpoint = checkpointRegister.newFieldCheckpoint(before ? InjectionPoint.BEFORE : InjectionPoint.AFTER, resolveType(null, owner), name,
                    resolveType(null, fieldClassName), opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD, details, source, latestLineNumber);
            super.visitLdcInsn(checkpoint.checkpointId());
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "checkpointReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
        }

    }

    private Class<?> resolveType(Class<?> maybeResolved, String owner) {
        if (maybeResolved != null) {
            return maybeResolved;
        }
        try {
            return ReflectionHelper.resolveType(owner);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
