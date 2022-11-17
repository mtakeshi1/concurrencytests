package concurrencytest.basic.asm;

import org.objectweb.asm.ClassVisitor;

public interface VisitorBuilderFunction {
    ClassVisitor buildFor(Class<?> targetClass, ClassVisitor delegate);
}
