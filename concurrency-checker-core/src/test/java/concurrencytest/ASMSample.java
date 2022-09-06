package concurrencytest;

import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class ASMSample implements Opcodes {

    public void write(MethodVisitor methodVisitor) {
        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();

        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLdcInsn(Type.getType("Lconcurrencytest/ClassTest;"));
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Class");
        methodVisitor.visitLdcInsn("syncStaticMethod");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitInsn(LCONST_1);
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/TestRuntimeImpl", "checkActualDispatchForStaticMethod", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)Z", false);
        methodVisitor.visitVarInsn(ISTORE, 2);

        methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
        methodVisitor.visitLabel(label0);
        methodVisitor.visitInsn(LCONST_1);
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitInsn(ACONST_NULL);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/ClassTest", "syncStaticMethod", "(JILjava/lang/Object;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitVarInsn(ILOAD, 2); // 2 is now the result of checkActual
        Label label6 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label6);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn(2L);
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/TestRuntimeImpl", "afterMonitorReleasedCheckpoint", "(Ljava/lang/Object;JLjava/lang/String;Ljava/lang/String;)V", false);
        Label label8 = new Label();
        methodVisitor.visitJumpInsn(GOTO, label8);
        methodVisitor.visitLabel(label6);
        methodVisitor.visitFrame(Opcodes.F_NEW, 4, new Object[] {"concurrencytest/ClassTest", "java/lang/Object", Opcodes.INTEGER, "java/lang/Object"}, 0, new Object[] {});
        methodVisitor.visitLdcInsn(3L);
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/TestRuntimeImpl", "checkpointReached", "(JLjava/lang/String;Ljava/lang/String;)V", false);
        methodVisitor.visitLabel(label8);
        methodVisitor.visitLabel(label1);

        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ARETURN);

        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitVarInsn(ILOAD, 2);
        Label label10 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label10);
        Label label11 = new Label();
        methodVisitor.visitLabel(label11);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn(2L);
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/TestRuntimeImpl", "afterMonitorReleasedCheckpoint", "(Ljava/lang/Object;JLjava/lang/String;Ljava/lang/String;)V", false);
        Label label12 = new Label();
        methodVisitor.visitJumpInsn(GOTO, label12);
        methodVisitor.visitLabel(label10);
        methodVisitor.visitLdcInsn(3L);
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitLdcInsn("");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "concurrencytest/TestRuntimeImpl", "checkpointReached", "(JLjava/lang/String;Ljava/lang/String;)V", false);
        methodVisitor.visitLabel(label12);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ATHROW);
        Label label13 = new Label();
        methodVisitor.visitLabel(label13);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label5, label2, 3);
        methodVisitor.visitLocalVariable("t", "Ljava/lang/Throwable;", null, label9, label13, 3);
        methodVisitor.visitLocalVariable("this", "Lconcurrencytest/ClassTest;", null, label3, label13, 0);
        methodVisitor.visitLocalVariable("maybeMonitor", "Ljava/lang/Object;", null, label4, label13, 1);
        methodVisitor.visitLocalVariable("is", "Z", null, label0, label13, 2);
        methodVisitor.visitMaxs(7, 4);
        methodVisitor.visitEnd();
    }


    public static void main(String[] args) throws Exception {
        ASMifier asMifier = new ASMifier();
        ClassReader reader = new ClassReader(ASMSample.class.getResourceAsStream("/" + ClassTest.class.getName().replace('.', '/') + ".class"));

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, asMifier, new PrintWriter(System.out));
        reader.accept(traceClassVisitor, ClassReader.EXPAND_FRAMES);
    }

}
