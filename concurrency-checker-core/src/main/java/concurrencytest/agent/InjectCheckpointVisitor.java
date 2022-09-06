package concurrencytest.agent;

import concurrencytest.TestRuntimeImpl;
import concurrencytest.annotations.CheckpointInjectionPoint;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.LongSupplier;

public class InjectCheckpointVisitor extends ClassVisitor {

    private final String className;

    private final LongSupplier checkpointIdGenerator;
    private final Set<String> unresolvedClassNames;
    private final Set<CheckpointInjectionPoint> injectionPoints;

    private volatile String sourceFileName = "unknown source";

    private static final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE, Type.getType(String.class), Type.getType(String.class));

    private static final String monitorDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.LONG_TYPE, Type.getType(String.class), Type.getType(String.class));

    private static final String lockDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Lock.class), Type.LONG_TYPE, Type.getType(String.class), Type.getType(String.class));

    private static final String checkDispatchDescriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.LONG_TYPE, Type.getType(String.class), Type.getType(String.class));
    private static final String checkStaticDispatchDescriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Class.class), Type.getType(String.class), Type.getType(String.class), Type.LONG_TYPE, Type.getType(String.class), Type.getType(String.class));

    private final Map<String, Class<?>> remmapedInternalNames;

    public InjectCheckpointVisitor(String className, LongSupplier checkpointIdGenerator, Map<String, Class<?>> remmapedInternalNames, Set<String> unresolvedClassNames, Collection<CheckpointInjectionPoint> injectionPoints) {
        this(new ClassWriter(ClassWriter.COMPUTE_FRAMES), className, checkpointIdGenerator, remmapedInternalNames, unresolvedClassNames, injectionPoints);
    }

    public InjectCheckpointVisitor(ClassVisitor delegate, String className, LongSupplier checkpointIdGenerator, Map<String, Class<?>> remmapedInternalNames, Set<String> unresolvedClassNames, Collection<CheckpointInjectionPoint> injectionPoints) {
        super(Opcodes.ASM7, delegate);
        this.className = className;
        this.checkpointIdGenerator = checkpointIdGenerator;
        this.remmapedInternalNames = remmapedInternalNames;
        this.unresolvedClassNames = unresolvedClassNames;
        this.injectionPoints = normalizeInjectionPoints(injectionPoints);
    }

    private Set<CheckpointInjectionPoint> normalizeInjectionPoints(Collection<CheckpointInjectionPoint> injectionPoints) {
        EnumSet<CheckpointInjectionPoint> set = EnumSet.noneOf(CheckpointInjectionPoint.class);
        if (injectionPoints.contains(CheckpointInjectionPoint.ALL)) {
            return EnumSet.allOf(CheckpointInjectionPoint.class);
        }
        set.addAll(injectionPoints);
        if (injectionPoints.contains(CheckpointInjectionPoint.FIELDS)) {
            set.add(CheckpointInjectionPoint.VOLATILE_FIELD_WRITE);
        }
        return set;
    }

    private Method tryResolveMethod(String owner, String name, String descriptor) {
        try {
            Class<?> host = tryResolveType(owner);
            Type[] argumentTypes = Type.getArgumentTypes(descriptor);
            Type returnType = Type.getReturnType(descriptor);

            for (Method m : host.getDeclaredMethods()) {
                if (m.getName().equals(name) && argumentTypes.length == m.getParameterCount() && typeMatch(returnType, m.getReturnType()) && allTypesMatch(argumentTypes, m.getParameterTypes())) {
                    return m;
                }
            }
        } catch (Throwable e) {
        }
        return null;
    }

    private boolean allTypesMatch(Type[] argumentTypes, Class<?>[] parameterTypes) {
        for (int i = 0; i < argumentTypes.length; i++) {
            if (!typeMatch(argumentTypes[i], parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean typeMatch(Type returnType, Class<?> methodReturnType) {
        return methodReturnType == tryResolveType(returnType.getInternalName());
    }

    private Class<?> tryResolveType(String ownerInternalName) {
        String className = Type.getObjectType(ownerInternalName).getClassName();
        if (unresolvedClassNames.contains(className)) {
            return null;
        }
        Class<?> host;
        if (remmapedInternalNames.get(ownerInternalName) != null) {
            host = remmapedInternalNames.get(ownerInternalName);
        } else {
            try {
                host = ReflectionHelper.resolveType(className);
            } catch (ClassNotFoundException | NoClassDefFoundError | UnsatisfiedLinkError | ExceptionInInitializerError e) {
                host = null;
            }
        }
        if (host == null) {
            unresolvedClassNames.add(className);
        }
        return host;
    }

    private static int minVersion15(int currentVersion) {
        switch (currentVersion) {
            case Opcodes.V1_1:
            case Opcodes.V1_2:
            case Opcodes.V1_3:
            case Opcodes.V1_4:
                return Opcodes.V1_5;
            default:
                return currentVersion;
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(minVersion15(version), access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.sourceFileName = source;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        int newAccessMods = access;
        if(injectionPoints.contains(CheckpointInjectionPoint.SYNCHRONIZED_METHODS)) {
            newAccessMods = newAccessMods & ~Modifier.SYNCHRONIZED;
        }
        MethodVisitor delegate = super.visitMethod(newAccessMods, name, descriptor, signature, exceptions);
        return new CheckpointMethodVisitor(delegate, name, access, descriptor, signature);
    }

    private class CheckpointMethodVisitor extends MethodVisitor {

        final String checkpointPreffix;
        private final String name;
        private final int originalAccessModifiers;
        private final String descriptor;
        private final String signature;

        private String lastInstructionDescription;

        int lastLine;
        int nextFreeLocalVariable;

        private final Map<Label, String> tryCatchCheckpoints;

        private Label outerTry, endTry, handler;

        public CheckpointMethodVisitor(MethodVisitor delegate, String name, int access, String descriptor, String signature) {
            super(Opcodes.ASM7, delegate);
            this.name = name;
            this.originalAccessModifiers = access;
            this.descriptor = descriptor;
            this.signature = signature;
            checkpointPreffix = className + "." + name;
            lastLine = -1;
            Type[] argumentTypes = Type.getMethodType(descriptor).getArgumentTypes();
            if (!Modifier.isStatic(access)) {
                nextFreeLocalVariable = 1;
            }
            for (int i = 0; i < argumentTypes.length; i++) {
                nextFreeLocalVariable += argumentTypes[i].getSize();
            }
            tryCatchCheckpoints = new HashMap<>();
        }

        @Override
        public void visitCode() {
            super.visitCode();
//            if(BehaviourModifier.isSynchronized(originalAccessModifiers) && injectionPoints.contains(CheckpointInjectionPoint.SYNCHRONIZED_METHODS)) {
//                outerTry = new Label();
//                endTry = new Label();
//                handler = new Label();
//                super.visitTryCatchBlock(outerTry, endTry, handler, Type.getInternalName(Throwable.class));
//                super.visitLabel(outerTry);
//            }
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            int x = var + 1;
            if (opcode == Opcodes.DLOAD || opcode == Opcodes.LLOAD || opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE) {
                x++;
            }
            nextFreeLocalVariable = Math.max(x, nextFreeLocalVariable);
            lastInstructionDescription = "<local " + var + ">";
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            super.visitFieldInsn(opcode, owner, name, descriptor);
            if ((opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) && fieldIsFinal(owner, name)) {
                //for final fields, we dont need to
                return;
            }
            //field loads, stores
            lastInstructionDescription = "field " + owner.replace('/', '.') + "." + name + " of type: " + Type.getType(descriptor).getClassName();
            String description;
            switch (opcode) {
                case Opcodes.GETFIELD:
                    description = "get_field: " + owner + "." + name + " - " + descriptor;
                    break;
                case Opcodes.PUTFIELD:
                    description = "put_field: " + owner + "." + name + " - " + descriptor;
                    break;
                case Opcodes.GETSTATIC:
                    description = "get_static_field: " + owner + "." + name + " - " + descriptor;
                    break;
                case Opcodes.PUTSTATIC:
                    description = "put_static_field: " + owner + "." + name + " - " + descriptor;
                    break;
                default:
                    description = "<unknown> " + owner + "." + name + " - " + descriptor;
            }
            if (injectionPoints.contains(CheckpointInjectionPoint.FIELDS) || (fieldIsVolatile(owner, name) && injectionPoints.contains(CheckpointInjectionPoint.VOLATILE_FIELD_WRITE))) {
                visitCheckpoint(description);
            }
        }

        private boolean fieldIsVolatile(String ownerInternalName, String name) {
            try {
                Class<?> type = tryResolveType(ownerInternalName);
                while (type != null) {
                    try {
                        Field field = type.getDeclaredField(name);
                        return Modifier.isVolatile(field.getModifiers());
                    } catch (NoSuchFieldException e) {
                    }
                    type = type.getSuperclass();
                }
            } catch (Throwable e) {
            }
            return false;
        }

        private boolean fieldIsFinal(String ownerInternalName, String name) {
            try {
                Class<?> type = tryResolveType(ownerInternalName);
                while (type != null) {
                    try {
                        Field field = type.getDeclaredField(name);
                        return Modifier.isFinal(field.getModifiers());
                    } catch (NoSuchFieldException e) {
                    }
                    type = type.getSuperclass();
                }
            } catch (Throwable e) {
            }
            return false;
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
            lastInstructionDescription = "";
        }

        @Override
        public void visitLdcInsn(Object value) {
            super.visitLdcInsn(value);
            lastInstructionDescription = "< constant: " + value + "> ";
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);
            lastInstructionDescription = "";
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            super.visitTableSwitchInsn(min, max, dflt, labels);
            lastInstructionDescription = "";
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            super.visitLookupSwitchInsn(dflt, keys, labels);
            lastInstructionDescription = "";
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            lastInstructionDescription = "";
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
            lastInstructionDescription = "";
        }


        private Object checkpointName() {
            return checkpointPreffix + "(" + sourceFileName + ":" + lastLine + ")";
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.MONITORENTER && injectionPoints.contains(CheckpointInjectionPoint.SYNCHRONIZED_BLOCKS)) {
                beforeMonitorCheckpoint("before monitor acquire on " + lastInstructionDescription);
                super.visitInsn(opcode);
            } else if (opcode == Opcodes.MONITOREXIT && injectionPoints.contains(CheckpointInjectionPoint.SYNCHRONIZED_BLOCKS)) {
                super.visitInsn(Opcodes.DUP);
                super.visitInsn(opcode);
                afterMonitorCheckpoint("after monitor released on " + lastInstructionDescription);
            } else if (opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD && injectionPoints.contains(CheckpointInjectionPoint.ARRAYS)) {
                super.visitInsn(opcode);
                visitCheckpoint("array_load on " + lastInstructionDescription);
            } else if (opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE && injectionPoints.contains(CheckpointInjectionPoint.ARRAYS)) {
                super.visitInsn(opcode);
                visitCheckpoint("array_store on " + lastInstructionDescription);
            } else if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                super.visitInsn(opcode);
            } else if (opcode == Opcodes.ATHROW && injectionPoints.contains(CheckpointInjectionPoint.EXCEPTION_THROWN)) {
                visitCheckpoint("throw " + lastInstructionDescription);
                super.visitInsn(opcode);
            } else {
                super.visitInsn(opcode);

            }
            lastInstructionDescription = "";
            //monitor enters / exits / array load store / return / throw
        }

        private void beforeMonitorCheckpoint(String description) {
            super.visitInsn(Opcodes.DUP);
            super.visitLdcInsn(checkpointIdGenerator.getAsLong());
            super.visitLdcInsn(checkpointName());
            super.visitLdcInsn(description);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "beforeMonitorAcquiredCheckpoint", monitorDescriptor, false);
        }

        private void afterMonitorCheckpoint(String description) {
            super.visitLdcInsn(checkpointIdGenerator.getAsLong());
            super.visitLdcInsn(checkpointName());
            super.visitLdcInsn(description);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "afterMonitorReleasedCheckpoint", monitorDescriptor, false);
        }

        private void beforeLockCheckpoint(String description) {
            super.visitLdcInsn(checkpointIdGenerator.getAsLong());
            super.visitLdcInsn(checkpointName());
            super.visitLdcInsn(description);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "beforeLockAcquiredCheckpoint", monitorDescriptor, false);
        }

        private void afterLockCheckpoint(String description) {
            super.visitLdcInsn(checkpointIdGenerator.getAsLong());
            super.visitLdcInsn(checkpointName());
            super.visitLdcInsn(description);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "afterLockReleasedCheckpoint", lockDescriptor, false);
        }

        private void visitCheckpoint(String namePreffix, String description) {
            long checkpointId = checkpointIdGenerator.getAsLong();
            super.visitLdcInsn(checkpointId);
            super.visitLdcInsn(namePreffix + checkpointName());
            super.visitLdcInsn(description);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "checkpointReached", methodDescriptor, false);
        }

        private void visitCheckpoint(String description) {
            this.visitCheckpoint("", description);
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            String description = tryCatchCheckpoints.get(label);
            if (description != null) {
                visitCheckpoint(description);
                tryCatchCheckpoints.remove(label);
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            super.visitTryCatchBlock(start, end, handler, type);
            if (injectionPoints.contains(CheckpointInjectionPoint.TRY_CATCH_BLOCK)) {
                tryCatchCheckpoints.put(handler, "catch ( " + type + " )");
                lastInstructionDescription = "";
            }
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start);
            this.lastLine = line;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            //unwind method call target and parameters into local variables, call something to actually resolve the method and check if its synchronized
            //add checkpoint if it is and record the result
            if (isUnmodifiable(owner) || isPure(owner, name, descriptor) || name.equals("<init>") || name.equals("<clinit>")) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }
//            if (Type.getInternalName(TestRuntimeImpl.class).equals(owner) && !injectionPoints.contains(CheckpointInjectionPoint.MANUAL)) {
//                unwind method arguments
//                Type[] argumentTypes = Type.getMethodType(descriptor).getArgumentTypes();
//                for (var argType : argumentTypes) {
//                    if (argType.getSize() == 2) {
//                        super.visitInsn(Opcodes.DUP2);
//                    } else {
//                        super.visitInsn(Opcodes.DUP);
//                    }
//                }
//                return;
//            }
            String methodDetails;
            switch (opcode) {
                case Opcodes.INVOKESTATIC:
                    methodDetails = "invoke static ";
                    break;
                case Opcodes.INVOKESPECIAL:
                    methodDetails = "invoke super/new ";
                    break;
                case Opcodes.INVOKEDYNAMIC:
                    methodDetails = "invoke dynamic ";
                    // check
                    break;
                case Opcodes.INVOKEVIRTUAL:
                    methodDetails = "";
                    break;
                case Opcodes.INVOKEINTERFACE:
                    methodDetails = "invoke interface ";
                    break;
                default:
                    methodDetails = "unknown call ";
                    break;
            }
            lastInstructionDescription = methodDetails + " " + owner.replace('/', '.') + "." + name + " ( " + descriptor + " ) ";
            if (opcode == Opcodes.INVOKEDYNAMIC || opcode == Opcodes.INVOKESPECIAL) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                visitCheckpoint(lastInstructionDescription);
                return;
            }
            if (opcode == Opcodes.INVOKESTATIC) {
                Method method = tryResolveMethod(owner, name, descriptor);
                if (method != null) {
                    if (Modifier.isSynchronized(method.getModifiers())) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        visitCheckpoint(lastInstructionDescription);
                        //TODO
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        visitCheckpoint(lastInstructionDescription);
                    }
                    return;
                }

            }
            Type methodType = Type.getType(descriptor);
            if (isMonitorLock(owner, name, descriptor) && injectionPoints.contains(CheckpointInjectionPoint.LOCKS)) {
                if (methodType.getArgumentTypes().length == 0) {
                    super.visitInsn(Opcodes.DUP);
                    beforeLockCheckpoint("before " + name + " " + lastInstructionDescription);
                } else {
                    int unitVar = nextFreeLocalVariable;
                    super.visitVarInsn(Opcodes.ASTORE, unitVar);
                    int timeoutVar = nextFreeLocalVariable + 1;
                    super.visitVarInsn(Opcodes.LSTORE, timeoutVar);
                    super.visitInsn(Opcodes.DUP);
                    beforeLockCheckpoint("before tryLock with timeout" + lastInstructionDescription);
                    // stack now only contains the lock
                    super.visitVarInsn(Opcodes.LLOAD, timeoutVar);
                    super.visitVarInsn(Opcodes.ALOAD, unitVar);
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            } else if (isMonitorUnlock(owner, name, descriptor) && injectionPoints.contains(CheckpointInjectionPoint.LOCKS)) {
                super.visitInsn(Opcodes.DUP);
                afterLockCheckpoint("after lock released: " + lastInstructionDescription);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            } else if (!injectionPoints.contains(CheckpointInjectionPoint.METHOD_CALL)) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            // the ith parameter is stored in localVariables[i] slot
            int[] localVariables = new int[methodType.getArgumentTypes().length];
            int localVar = nextFreeLocalVariable; // this will also contain the result of the dispatch
            for (int i = methodType.getArgumentTypes().length - 1; i >= 0; i--) {
                Type argType = methodType.getArgumentTypes()[i];
                localVariables[i] = localVar;
                localVar += argType.getSize();
                super.visitVarInsn(methodType.getArgumentTypes()[i].getOpcode(Opcodes.ISTORE), localVariables[i]);
            }
            Object callTargetReference;
            //stack now contains the call target or nothing if its a static method
            if (opcode == Opcodes.INVOKESTATIC) {
                // stack is empty so we push the target class
                super.visitLdcInsn(Type.getObjectType(owner));
                callTargetReference = Type.getObjectType(owner);
            } else {
                super.visitInsn(Opcodes.DUP);
                super.visitVarInsn(Opcodes.ASTORE, localVar);
                callTargetReference = localVar;
                localVar++;
            }
            //stack now contains the call target or the class if its a static method
            super.visitLdcInsn(name);
            super.visitLdcInsn(descriptor);
            super.visitLdcInsn(checkpointIdGenerator.getAsLong());
            super.visitLdcInsn(checkpointName());
            super.visitLdcInsn(lastInstructionDescription);

            if (opcode == Opcodes.INVOKESTATIC) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "checkActualDispatchForStaticMethod", checkStaticDispatchDescriptor, false);
            } else {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TestRuntimeImpl.class), "checkActualDispatchForMonitor", checkDispatchDescriptor, false);

            }
            // the stack now has a boolean / int and we must store on a local variable
            super.visitVarInsn(Opcodes.ISTORE, localVar);
//            // we now push the method arguments back
            Label begin = new Label();
            Label end = new Label();
            Label handler = new Label();
            Label afterHandler = new Label();
            super.visitTryCatchBlock(begin, end, handler, "java/lang/Throwable");
            if (callTargetReference instanceof Integer) {
                super.visitVarInsn(Opcodes.ALOAD, (Integer) callTargetReference);
            }
            for (int i = 0; i <= methodType.getArgumentTypes().length - 1; i++) {
                super.visitVarInsn(methodType.getArgumentTypes()[i].getOpcode(Opcodes.ILOAD), localVariables[i]);
            }
            super.visitLabel(begin);
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//            // now the stack is either empty (void returning methods) or contains the return value
            generateIfElseReleaseMonitor(localVar, callTargetReference);
            super.visitJumpInsn(Opcodes.GOTO, afterHandler);
            super.visitLabel(end);

//            // catch (Throwable t)
            super.visitLabel(handler);
            super.visitVarInsn(Opcodes.ASTORE, localVar + 1); // localVar +1 has a throwable
            // now the stack contains a Throwable
            generateIfElseReleaseMonitor(localVar, callTargetReference);
            super.visitVarInsn(Opcodes.ALOAD, localVar + 1); // we throw it to the caller
            super.visitInsn(Opcodes.ATHROW);

            super.visitLabel(afterHandler);
        }


        private void generateIfElseReleaseMonitor(int localVar, Object callTarget) {
            super.visitVarInsn(Opcodes.ILOAD, localVar); // localvar has (still) the result of the call
            Label elseLabel = new Label();
            Label ifelseExit = new Label();
            super.visitJumpInsn(Opcodes.IFEQ, elseLabel);
            // if( dispatch == true ) - in other words, the method call was a synchronized one
            if (callTarget instanceof Type) {
                super.visitLdcInsn(callTarget);
            } else {
                super.visitVarInsn(Opcodes.ALOAD, (Integer) callTarget);
            }
            afterMonitorCheckpoint("after monitor released on " + lastInstructionDescription);
            super.visitJumpInsn(Opcodes.GOTO, ifelseExit);
            // else block
            super.visitLabel(elseLabel);
            // the method was not dispatched to a synchronized so we do a plain checkpoint
            visitCheckpoint(lastInstructionDescription);
            super.visitLabel(ifelseExit);
        }

        private boolean isMonitorUnlock(String owner, String name, String descriptor) {
            Class<?> resolveType = tryResolveType(owner);
            return resolveType != null && Lock.class.isAssignableFrom(resolveType) && ("unlock".equals(name));
        }

        private boolean isMonitorLock(String owner, String name, String descriptor) {
            Class<?> resolveType = tryResolveType(owner);
            return resolveType != null && Lock.class.isAssignableFrom(resolveType) && ("lock".equals(name) || "lockInterruptibly".equals(name) || "tryLock".equals(name));
        }

        private boolean isPure(String owner, String name, String descriptor) {
            return owner.equals("java/lang/Math") || (name.equals("getClass") && Type.getMethodType(descriptor).getArgumentTypes().length == 0) || owner.equals("java/lang/System") || (owner.equals("java/lang/Object") && name.equals("<init>"));
        }

        private boolean isUnmodifiable(String owner) {
            Class<?> resolveType = tryResolveType(owner);
            return resolveType != null && (resolveType == String.class || ReflectionHelper.isPrimitiveWrapper(resolveType) || ReflectionHelper.isUnmodifiableCollection(resolveType));
        }

    }
}
