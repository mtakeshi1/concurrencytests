package concurrencytest.asm;

import org.objectweb.asm.ClassVisitor;

public interface VisitorBuilderFunction {
    ClassVisitor buildFor(Class<?> targetClass, ClassVisitor delegate);
}
