package concurrencytest.asm.utils;

import concurrencytest.reflection.ReflectionHelper;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ReadClassesVisitor extends ClassVisitor {

    private Set<Class<?>> discoveredClasses = new HashSet<>();

    public ReadClassesVisitor() {
        super(Opcodes.ASM9);
    }

    public ReadClassesVisitor(Set<Class<?>> discoveredClasses) {
        super(Opcodes.ASM9);
        this.discoveredClasses = discoveredClasses;
    }

    public Set<Class<?>> getDiscoveredClasses() {
        return discoveredClasses;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        typeDiscovered(Type.getObjectType(name));
        if (superName != null) {
            typeDiscovered(Type.getObjectType(superName));
        }
        if (interfaces != null) {
            for (String iName : interfaces) {
                typeDiscovered(Type.getObjectType(iName));
            }
        }
    }

    private void typeDiscovered(Class<?> type) {
        if (type == null || discoveredClasses.contains(type)) {
            return;
        } else if (type.isArray()) {
            typeDiscovered(type.getComponentType());
        } else {
            discoveredClasses.add(type);
        }
        for (Class<?> inner : type.getDeclaredClasses()) {
            typeDiscovered(inner);
        }
        for (Class<?> implementedInterfaces : type.getInterfaces()) {
            typeDiscovered(implementedInterfaces);
        }
        if (type.getSuperclass() != null) {
            typeDiscovered(type.getSuperclass());
        }
        for (Field field : type.getDeclaredFields()) {
            typeDiscovered(field.getType());
        }
    }

    private void typeDiscovered(Type type) {
        try {
            typeDiscovered(ReflectionHelper.resolveType(type.getClassName()));
        } catch (ClassNotFoundException e) {
//            System.err.println("Could not find class: " + type.getClassName());
//            throw new RuntimeException(e);
        } catch (NoClassDefFoundError | UnsatisfiedLinkError | ExceptionInInitializerError e) {
//            System.err.println("Could not find dependency for class: " + type.getClassName() + " - " + e.getMessage());
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        typeDiscovered(Type.getType(descriptor));
        return createAnnotationVisitor(descriptor, visible);
    }

    private AnnotationVisitor createAnnotationVisitor(String descriptor, boolean visible) {
        return new AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(descriptor, visible)) {

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                typeDiscovered(Type.getType(descriptor));
                super.visitEnum(name, descriptor, value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                typeDiscovered(Type.getType(descriptor));
                return createAnnotationVisitor(descriptor, visible);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return super.visitArray(name);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Type methodType = Type.getMethodType(descriptor);
        for (Type param : methodType.getArgumentTypes()) {
            typeDiscovered(param);
        }
        typeDiscovered(methodType.getReturnType());
        if (exceptions != null) {
            for (String excName : exceptions) {
                typeDiscovered(Type.getObjectType(excName));
            }
        }
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                super.visitMultiANewArrayInsn(descriptor, numDimensions);
                typeDiscovered(Type.getType(descriptor));
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                super.visitTypeInsn(opcode, type);
                typeDiscovered(Type.getObjectType(type));
            }

            @Override
            public void visitLdcInsn(Object value) {
                super.visitLdcInsn(value);
                if (value instanceof Type) {
                    typeDiscovered((Type) value);
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                typeDiscovered(Type.getObjectType(owner));
                Type methodType = Type.getMethodType(descriptor);
                for (Type arg : methodType.getArgumentTypes()) {
                    typeDiscovered(arg);
                }
                typeDiscovered(methodType.getReturnType());
            }
        };
    }
}
