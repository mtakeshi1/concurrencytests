package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Member;

public class BaseClassVisitor extends ClassVisitor {

    protected final CheckpointRegister checkpointRegister;
    protected final Class<?> classUnderEnhancement;
    protected final ClassResolver classResolver;
    protected String sourceName;

    public BaseClassVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(Opcodes.ASM9, delegate);
        this.checkpointRegister = register;
        this.classUnderEnhancement = classUnderEnhancement;
        this.classResolver = classResolver;
        this.sourceName = classUnderEnhancement.getName();
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        sourceName = source;
    }

    protected Member resolveMethodOrConstructor(Class<?> callTarget, String methodName, String descriptor) {
        return classResolver.resolveMethodOrConstructor(callTarget, methodName, descriptor);
    }

    protected final Class<?> resolveType(Class<?> maybeResolved, String owner) {
        if (maybeResolved != null) {
            return maybeResolved;
        }
        return classResolver.resolveName(owner);
    }

}
