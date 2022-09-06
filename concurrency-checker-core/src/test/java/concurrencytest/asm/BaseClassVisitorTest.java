package concurrencytest.asm;

import concurrencytest.CheckpointRuntimeAccessor;
import concurrencytest.agent.OpenClassLoader;
import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.testClasses.AnnotationTarget;
import concurrencytest.checkpoint.*;
import concurrencytest.util.ASMUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BaseClassVisitorTest {

    public static final AtomicLong ID_GENERATOR = new AtomicLong();

    public static class StandardCheckpointRegister implements CheckpointRegister {

        private Map<Long, Checkpoint> allCheckpoints = new HashMap<>();

        private final AtomicLong idGenerator = new AtomicLong();

        @Override
        public FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber) {

            FieldAccessCheckpointImpl fieldAccessCheckpoint = new FieldAccessCheckpointImpl(
                    idGenerator.incrementAndGet(), injectionPoint, details, sourceFile, lineNumber, declaringClass, fieldType, fieldName, read
            );
            allCheckpoints.put(fieldAccessCheckpoint.checkpointId(), fieldAccessCheckpoint);
            return fieldAccessCheckpoint;
        }

        @Override
        public Map<Long, Checkpoint> allCheckpoints() {
            return allCheckpoints;
        }
    }

    public static class ManagedRuntime implements CheckpointRuntime {

        private final CheckpointRegister checkpointRegister;

        private List<Checkpoint> checkpoints = new ArrayList<>();

        public List<Checkpoint> getCheckpoints() {
            return checkpoints;
        }

        public ManagedRuntime(CheckpointRegister checkpointRegister) {
            this.checkpointRegister = checkpointRegister;
        }

        @Override
        public void checkpointReached(long id) {
            Checkpoint checkpoint = checkpointRegister.checkpointById(id);
            Assert.assertNotNull("checkpoint not found: " + id, checkpoint);
            checkpoints.add(checkpoint);
        }

        @Override
        public void fieldAccessCheckpoint(long checkpointId, Object owner, Object value) {
            throw new RuntimeException("not yet implemented");
        }
    }

    public <E> ManagedRuntime execute(E instance, Consumer<E> execution) {
        ManagedRuntime runtime = new ManagedRuntime(register);
        CheckpointRuntimeAccessor.setup(runtime);
        execution.accept(instance);
        return runtime;
    }

    CheckpointRegister register = new StandardCheckpointRegister();

    interface VisitorBuilderFunction {
        ClassVisitor buildFor(Class<?> targetClass, ClassVisitor delegate);
    }

    public Class<?> prepare(Class<?> target, VisitorBuilderFunction factory) throws IOException, ClassNotFoundException {
        ClassReader reader = ASMUtils.readClass(target);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String newName = target.getName() + "$$Injected_" + ID_GENERATOR.incrementAndGet();
        String oldInternalName = Type.getType(target).getInternalName();
        String newInternalName = newName.replace('.', '/');
        ClassRemapper map = new ClassRemapper(writer, new SimpleRemapper(oldInternalName, newInternalName));
        ClassVisitor visitor = factory.buildFor(target, map);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        byte[] byteCode = writer.toByteArray();
        OpenClassLoader loader = new OpenClassLoader();
        loader.addClass(newName, byteCode);
        return Class.forName(newName, true, loader);
    }


    @Test
    public void testNoOp() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> prepared = prepare(AnnotationTarget.class, (t, cv) -> cv);
        Runnable run = (Runnable) prepared.getConstructor().newInstance();
        run.run();
    }
}