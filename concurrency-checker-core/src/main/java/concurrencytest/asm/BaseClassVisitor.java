package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;

public class BaseClassVisitor extends ClassVisitor {

    protected final CheckpointRegister checkpointRegister;
    protected final Class<?> classUnderEnhancement;
    protected final ClassResolver classResolver;
    protected String sourceName;

    public BaseClassVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(Opcodes.ASM7, delegate);
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
        Type type = Type.getMethodType(descriptor);
        Type[] argTypes = type.getArgumentTypes();
        Class<?>[] params = new Class[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            params[i] = resolveType(null, argTypes[i].getClassName());
        }
        try {
            if (methodName.equals("<init>")) {
                return callTarget.getDeclaredConstructor(params);
            }
            return resolveMethodRecursive(callTarget, methodName, params);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Executable resolveMethodRecursive(Class<?> callTarget, String methodName, Class<?>[] params) throws NoSuchMethodException {
        try {
            return callTarget.getDeclaredMethod(methodName, params);
        } catch (NoSuchMethodException e) {
            if (callTarget.getSuperclass() == null && callTarget.getInterfaces().length == 0) {
                throw e;
            }
            for (Class<?> implementedInterface : callTarget.getInterfaces()) {
                try {
                    resolveMethodRecursive(implementedInterface, methodName, params);
                } catch (NoSuchMethodException ex) {
                    //ignore
                }
            }
            return resolveMethodRecursive(callTarget.getSuperclass(), methodName, params);
        }
    }

    protected final Class<?> resolveType(Class<?> maybeResolved, String owner) {
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
