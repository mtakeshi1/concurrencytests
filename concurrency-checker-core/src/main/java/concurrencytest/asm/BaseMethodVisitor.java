package concurrencytest.asm;

import concurrencytest.CheckpointRuntimeAccessor;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.Collection;

public abstract class BaseMethodVisitor extends MethodVisitor {
    private final CheckpointRegister checkpointRegister;
    protected final String sourceName;
    private final int allModifiers;
    protected final String methodName;
    protected final String methodDescriptor;
    protected final AccessModifier accessModifier;

    protected int latestLineNumber = -1;
    protected Label latestLabel;

    protected int nextFreeLocalVariable;

    protected final Collection<BehaviourModifier> modifiers;

    public BaseMethodVisitor(MethodVisitor delegate, CheckpointRegister register, String sourceName, int modifiers, String methodName, String descriptor) {
        super(Opcodes.ASM7, delegate);
        this.checkpointRegister = register;
        this.sourceName = sourceName;
        this.allModifiers = modifiers;
        this.methodName = methodName;
        this.methodDescriptor = descriptor;
        if (!Modifier.isStatic(modifiers)) {
            this.nextFreeLocalVariable = 1;
        }
        this.modifiers = BehaviourModifier.unreflect(modifiers);
        this.accessModifier = AccessModifier.unreflect(modifiers);
    }

    protected Type peekStackType() {
        return Type.getType(Object.class);
    }

    protected void invokeStringCheckpointWithContext(Checkpoint checkpoint) {
        super.visitLdcInsn(checkpoint.checkpointId());
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "checkpointWithMessageReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.INT_TYPE), false);
    }

    protected void invokeGenericCheckpointWithContext(Checkpoint checkpoint) {
        super.visitLdcInsn(checkpoint.checkpointId());
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "genericCheckpointReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.INT_TYPE), false);
    }

    //genericCheckpointReached
    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        int next = switch (opcode) {
            case Opcodes.DLOAD, Opcodes.LLOAD, Opcodes.DSTORE, Opcodes.LSTORE -> var + 2;
            default -> var + 1;
        };
        nextFreeLocalVariable = Math.max(next, nextFreeLocalVariable);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        this.latestLineNumber = line;
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        latestLabel = label;
    }

    protected void invokeEmptyCheckpoint(Checkpoint checkpoint) {
        super.visitLdcInsn(checkpoint.checkpointId());
        super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(CheckpointRuntimeAccessor.class), "checkpointReached", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
    }
}
