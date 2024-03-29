package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.config.FieldAccessMatch;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;

/**
 * A ClassVisitor that matches field reads / writes on methods declared inside.
 */
public class FieldCheckpointVisitor extends BaseClassVisitor {
    //TODO check if we need something like a classresolver
    private final FieldAccessMatch checkpointMatchConfiguration;

    public FieldCheckpointVisitor(FieldAccessMatch fieldCheckpoint, ClassVisitor delegate, CheckpointRegister checkpointRegister, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        this.checkpointMatchConfiguration = fieldCheckpoint;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new FieldCheckpointMethodVisitor(sourceName, super.visitMethod(access, name, descriptor, signature, exceptions), access, name, descriptor);
    }

    private class FieldCheckpointMethodVisitor extends BaseMethodVisitor {

        public FieldCheckpointMethodVisitor(String sourceName, MethodVisitor delegate, int access, String methodName, String descriptor) {
            super(classUnderEnhancement, delegate, FieldCheckpointVisitor.this.checkpointRegister, sourceName, access, methodName, descriptor, FieldCheckpointVisitor.this.classResolver);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String ownerClassName = owner.replace('/', '.');
            String fieldTypeClassName = Type.getType(descriptor).getClassName();
            Class<?> ownerType = classResolver.resolveName(ownerClassName);
            Field field = classResolver.lookupField(ownerType, name);
            Class<?> fieldType = classResolver.resolveName(fieldTypeClassName);
            if (FieldCheckpointVisitor.this.checkpointMatchConfiguration.matches(classUnderEnhancement,
                    ownerType, name, fieldType, field.getModifiers(), opcode, InjectionPoint.BEFORE)) {
                injectCheckpoint(opcode, ownerClassName, fieldTypeClassName, name, true);
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
            if (FieldCheckpointVisitor.this.checkpointMatchConfiguration.matches(classUnderEnhancement,
                    ownerType, name, classResolver.resolveName(fieldTypeClassName), field.getModifiers(), opcode, InjectionPoint.AFTER)) {
                injectCheckpoint(opcode, ownerClassName, fieldTypeClassName, name, false);
            }
        }

        private void injectCheckpoint(int opcode, String owner, String fieldClassName, String name, boolean before) {
            String details = String.format("%s %s %s.%s", before ? "BEFORE" : "AFTER", ReflectionHelper.renderOpcode(opcode), owner, name);
            Checkpoint checkpoint = checkpointRegister.newFieldCheckpoint(before ? InjectionPoint.BEFORE : InjectionPoint.AFTER, Type.getObjectType(owner), name,
                    Type.getObjectType(fieldClassName), opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD, details, sourceName, latestLineNumber);
            super.invokeEmptyCheckpoint(checkpoint);
        }

    }


}
