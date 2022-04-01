package concurrencytest.agent.bytecode;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

public class InjectCheckpointsVisitor extends ClassVisitor {

    private final String className;

    private final LongSupplier checkpointIdGenerator;
    private final Set<String> unresolvedClassNames;
    private final Map<String, Class<?>> remmapedInternalNames;
    private volatile String sourceFileName = "unknown source";

    public InjectCheckpointsVisitor(String className, LongSupplier checkpointIdGenerator, Map<String, Class<?>> remmapedInternalNames, Set<String> unresolvedClassNames) {
        this(new ClassWriter(ClassWriter.COMPUTE_FRAMES), className, checkpointIdGenerator, remmapedInternalNames, unresolvedClassNames);
    }

    public InjectCheckpointsVisitor(ClassVisitor delegate, String className, LongSupplier checkpointIdGenerator, Map<String, Class<?>> remmapedInternalNames, Set<String> unresolvedClassNames) {
        super(Opcodes.ASM7, delegate);
        this.className = className;
        this.checkpointIdGenerator = checkpointIdGenerator;
        this.remmapedInternalNames = remmapedInternalNames;
        this.unresolvedClassNames = unresolvedClassNames;
    }

}
