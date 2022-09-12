package concurrencytest.asm;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.util.ClassResolver;
import concurrencytest.util.ReflectionHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class BaseClassVisitor extends ClassVisitor {

    protected final CheckpointRegister checkpointRegister;
    protected final Class<?> classUnderEnhancement;
    protected final ClassResolver classResolver;
    protected String sourceName = "unkown";

    public BaseClassVisitor(ClassVisitor delegate, CheckpointRegister register, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(Opcodes.ASM7, delegate);
        this.checkpointRegister = register;
        this.classUnderEnhancement = classUnderEnhancement;
        this.classResolver = classResolver;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        sourceName = source;
    }

    protected Class<?> resolveType(Class<?> maybeResolved, String owner) {
        if (maybeResolved != null) {
            return maybeResolved;
        }
        try {
            return ReflectionHelper.resolveType(owner);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
