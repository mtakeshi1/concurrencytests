package concurrencytest;

import concurrencytest.asm.testClasses.SyncBlockTarget;
import concurrencytest.asm.testClasses.SyncCallable;
import org.objectweb.asm.*;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class ASMSample implements Opcodes {

    public void write(MethodVisitor methodVisitor) {
        methodVisitor.visitCode();
        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        methodVisitor.visitTryCatchBlock(label0, label1, label2, null);
        Label label3 = new Label();
        methodVisitor.visitTryCatchBlock(label2, label3, label2, null);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ASTORE, 4);
        methodVisitor.visitInsn(MONITORENTER);

        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(DLOAD, 1);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "concurrencytest/asm/testClasses/SyncCallable", "execute$delegate", "(DI)Ljava/lang/Long;", false);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitInsn(MONITOREXIT);
        methodVisitor.visitLabel(label1);

        methodVisitor.visitInsn(ARETURN);

        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ASTORE, 5);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitInsn(MONITOREXIT);

        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitInsn(ATHROW);
        methodVisitor.visitMaxs(4, 6);
        methodVisitor.visitEnd();
    }

    public void bla$a() {

    }

    public static void main(String[] args) throws Exception {
        ASMifier asMifier = new ASMifier();
        ClassReader reader = new ClassReader(SyncBlockTarget.class.getResourceAsStream("/" + SyncBlockTarget.class.getName().replace('.', '/') + ".class"));

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, asMifier, new PrintWriter(System.out));
        reader.accept(traceClassVisitor, ClassReader.EXPAND_FRAMES);
    }

}
