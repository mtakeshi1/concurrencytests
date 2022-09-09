package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public abstract class BaseMethodVisitor extends MethodVisitor {
    private final CheckpointRegister checkpointRegister;
    private final String sourceName;
    private final int modifiers;

    protected int latestLineNumber = -1;
    protected Label latestLabel;

    protected int nextFreeLocalVariable;

    public BaseMethodVisitor(MethodVisitor delegate, CheckpointRegister register, String sourceName, int modifiers) {
        super(Opcodes.ASM7, delegate);
        this.checkpointRegister = register;
        this.sourceName = sourceName;
        this.modifiers = modifiers;
        if (!Modifier.isStatic(modifiers)) {
            this.nextFreeLocalVariable = 1;
        }
    }

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
}
