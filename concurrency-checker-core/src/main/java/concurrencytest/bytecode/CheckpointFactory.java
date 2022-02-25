package concurrencytest.bytecode;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class CheckpointFactory extends MethodVisitor {

    final String checkpointPreffix;
    final int originalAccessModifiers;
    final String descriptor;
    final String signature;
    final String methodName;
    final String declaringClassName;
    final String sourceFileName;
    MethodVisitor methodWriter;

    String lastInstructionDescription;

    int lastLine;
    int nextFreeLocalVariable;

    public CheckpointFactory(MethodVisitor methodWriter, MethodVisitor delegate, String ownerName, String name, int access, String descriptor, String signature, String sourceFileName) {
        super(Opcodes.ASM7, delegate);
        this.methodWriter = methodWriter;
        this.methodName = name;
        this.declaringClassName = ownerName;
        this.checkpointPreffix = ownerName + "." + name;
        this.originalAccessModifiers = access;
        this.descriptor = descriptor;
        this.signature = signature;
        this.sourceFileName = sourceFileName;
    }


    @Override
    public final void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        int x = var + 1;
        if (opcode == Opcodes.DLOAD || opcode == Opcodes.LLOAD || opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE) {
            x++;
        }
        nextFreeLocalVariable = Math.max(x, nextFreeLocalVariable);
        lastInstructionDescription = "<local " + var + ">";
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

    protected final Object checkpointName() {
        return checkpointPreffix + "(" + sourceFileName + ":" + lastLine + ")";
    }
}
