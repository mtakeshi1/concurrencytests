package concurrencytest.bytecode;

import org.objectweb.asm.MethodVisitor;

public interface CheckpointVisitorFactory {

    CheckpointFactory createMethodVisitor(MethodVisitor methodWriter, MethodVisitor nextOnChain);


}
