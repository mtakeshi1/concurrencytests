package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

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
            for (var m : callTarget.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && paramsMatch(m, params)) {
                    return m;
                }
            }
            if (callTarget.getSuperclass() == null && callTarget.getInterfaces().length == 0) {
                throw e;
            }
            for (Class<?> implementedInterface : callTarget.getInterfaces()) {
                try {
                    var m = resolveMethodRecursive(implementedInterface, methodName, params);
                    if (m != null) {
                        return m;
                    }
                } catch (NoSuchMethodException ex) {
                    //ignore
                }
            }
            if (callTarget.getSuperclass() != null) {
                return resolveMethodRecursive(callTarget.getSuperclass(), methodName, params);
            }
            return null;
        }
    }

    private static boolean paramsMatch(Method m, Class<?>[] params) {
        if (m.getParameterCount() > params.length) {
            return false;
        }
        int count = m.getParameterCount();
        if (m.isVarArgs()) {
            count--;
        }
        Class<?>[] types = m.getParameterTypes();
        int i;
        for (i = 0; i < count; i++) {
            if(!types[i].isAssignableFrom(params[i])) {
                return false;
            }
        }
        if (m.isVarArgs()) {
            Class<?> lastType = types[types.length - 1].componentType();
            for(; i < params.length; i++) {
                if(!lastType.isAssignableFrom(params[i])) {
                    return false;
                }
            }
        }
        return true;
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
