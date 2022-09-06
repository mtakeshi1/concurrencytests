package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class BaseMethodVisitor extends MethodVisitor {
    private final CheckpointRegister checkpointRegister;
    private final String sourceName;

    protected int latestLineNumber = -1;
    protected Label latestLabel;

    public BaseMethodVisitor(MethodVisitor delegate, CheckpointRegister register, String sourceName) {
        super(Opcodes.ASM7, delegate);
        this.checkpointRegister = register;
        this.sourceName = sourceName;
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
