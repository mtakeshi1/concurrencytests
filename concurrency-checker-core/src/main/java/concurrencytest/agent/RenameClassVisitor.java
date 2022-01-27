package concurrencytest.agent;

import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RenameClassVisitor extends ClassVisitor {

    private final ClassWriter writer;

    private final Map<String, String> classNames = new HashMap<>();
    private final Map<String, String> internalNames = new HashMap<>();
    private final Map<String, String> typeDescriptors = new HashMap<>();

    public RenameClassVisitor(Set<Class<?>> classesToBeRenamed, String suffix) {
        super(Opcodes.ASM7, new ClassWriter(ClassWriter.COMPUTE_FRAMES));
        this.writer = (ClassWriter) super.cv;
        for (var cl : classesToBeRenamed) {
            String internalName = Type.getInternalName(cl);
            classNames.put(cl.getName(), cl.getName() + suffix);
            internalNames.put(internalName, internalName + suffix);
            typeDescriptors.put(Type.getDescriptor(cl), "L" + internalName + suffix + ";");
        }
    }

    public byte[] getBytecode() {
        return writer.toByteArray();
    }

    private String renameInternalName(String original) {
        if (original == null) {
            return null;
        }
        String renamed = internalNames.get(original);
        if (renamed != null) {
            return renamed;
        }
        if (original.endsWith("[]")) {
            //is array
            String arrayComponent = original.substring(0, original.length() - 2);
            return renameInternalName(arrayComponent) + "[]";
        }
        return original;
    }

    private String renameClassName(String original) {
        if (original == null || ReflectionHelper.isPrimitive(original)) {
            return null;
        }
        String renamed = classNames.get(original);
        if (renamed != null) {
            return renamed;
        }
        if (original.endsWith("[]")) {
            //is array
            String arrayComponent = original.substring(0, original.length() - 2);
            return renameClassName(arrayComponent) + "[]";
        } else if (original.startsWith("[")) {
            String arrayComponent = original.substring(1);
            return "";
        }
        return original;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, renameInternalName(name), signature, renameInternalName(superName), renameAllInternalNames(interfaces));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Type type = renameType(Type.getType(descriptor));
        return super.visitField(access, name, type.getDescriptor(), signature, value);
    }

    private String[] renameAllInternalNames(String[] interfaces) {
        if (interfaces == null || interfaces.length == 0) {
            return interfaces;
        }
        String[] renamed = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            renamed[i] = renameInternalName(interfaces[i]);
        }
        return renamed;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Type[] args = renameTypes(Type.getArgumentTypes(descriptor));
        Type ret = renameType(Type.getReturnType(descriptor));
        String[] exceptionsRenamed = renameAllInternalNames(exceptions);

        MethodVisitor delegate = super.visitMethod(access, name, Type.getMethodDescriptor(ret, args), signature, exceptionsRenamed);
        return new MethodVisitor(Opcodes.ASM7, delegate) {
            @Override
            public void visitTypeInsn(int opcode, String type) {
                super.visitTypeInsn(opcode, renameInternalName(type));
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    super.visitLdcInsn(renameType((Type) value));
                } else if (value instanceof String) {
                    String newClassName = classNames.getOrDefault((String) value, (String) value);
                    super.visitLdcInsn(newClassName);
                } else {
                    super.visitLdcInsn(value);
                }
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                Type type = renameType(Type.getType(descriptor));
                super.visitFieldInsn(opcode, renameInternalName(owner), name, type.getDescriptor());
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                Type[] args = renameTypes(Type.getArgumentTypes(descriptor));
                Type ret = renameType(Type.getReturnType(descriptor));
                super.visitMethodInsn(opcode, renameInternalName(owner), name, Type.getMethodDescriptor(ret, args), isInterface);
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                super.visitLocalVariable(name, renameType(Type.getType(descriptor)).getDescriptor(), signature, start, end, index);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                super.visitTryCatchBlock(start, end, handler, renameInternalName(type));
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                super.visitMultiANewArrayInsn(renameType(Type.getType(descriptor)).getDescriptor(), numDimensions);
            }

        };
    }

    private Type[] renameTypes(Type[] argumentTypes) {
        if (argumentTypes == null || argumentTypes.length == 0) {
            return argumentTypes;
        }
        Type[] renamed = new Type[argumentTypes.length];
        for (int i = 0; i < renamed.length; i++) {
            Type type = argumentTypes[i];
            Type renamedType = renameType(type);
            renamed[i] = renamedType;
        }
        return renamed;
    }

    private Type renameType(Type type) {
        if (ReflectionHelper.isPrimitive(type.getClassName())) {
            return type;
        } else if (type.getSort() == Type.ARRAY) {
            if (type.getDescriptor().startsWith("[")) { // it should always be true
                String componentTypeDescriptor = type.getDescriptor().substring(1);
                Type componentType = renameType(Type.getType(componentTypeDescriptor));
                return Type.getType("[" + componentType.getDescriptor());
            } else {
                throw new RuntimeException("type is marked as array but doesn't have the '[' preffix: " + type.getDescriptor());
            }
        } else {
            String newInternalName = renameClassName(type.getClassName());
            return Type.getObjectType(newInternalName.replace('.', '/'));
        }
    }
}
