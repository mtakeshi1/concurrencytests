package concurrencytest.asm;

import concurrencytest.runtime.CheckpointRuntimeAccessor;
import concurrencytest.agent.OpenClassLoader;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.RecordingCheckpointRuntime;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.util.ASMUtils;
import org.junit.Assert;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class BaseClassVisitorTest {

    private static int idSeed;

    public interface ConsumerWithError<T> {
        void accept(T instance) throws Exception;
    }

    public <E> RecordingCheckpointRuntime execute(E instance, ConsumerWithError<E> execution) throws Exception {
        RecordingCheckpointRuntime runtime = new RecordingCheckpointRuntime(register);
        CheckpointRuntimeAccessor.setup(runtime);
        execution.accept(instance);
        return runtime;
    }

    protected CheckpointRegister register = new StandardCheckpointRegister();

    public Class<?> prepare(Class<?> target, VisitorBuilderFunction factory) throws Exception {
        try {
            return prepare(target, factory, false);
        } catch (Exception e) {
            prepare(target, factory, true);
            throw e;
        }
    }

    public Class<?> prepare(Class<?> target, VisitorBuilderFunction factory, boolean dump) throws Exception {
        ClassReader reader = ASMUtils.readClass(target);
        Assert.assertNotNull(reader);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor delegate;
        if (dump) {
            delegate = new TraceClassVisitor(writer, new PrintWriter(System.out));
        } else {
            delegate = writer;
        }
        String newName = target.getName() + "$$Injected_" + idSeed++;
        String oldInternalName = Type.getType(target).getInternalName();
        String newInternalName = newName.replace('.', '/');
        ClassRemapper map = new ClassRemapper(new CheckClassAdapter(delegate), new SimpleRemapper(oldInternalName, newInternalName));
        ClassVisitor visitor = factory.buildFor(target, map);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        byte[] byteCode = writer.toByteArray();
        OpenClassLoader loader = new OpenClassLoader();
        loader.addClass(newName, byteCode);
        Class<?> name = Class.forName(newName, true, loader);
        name.getConstructor().newInstance();
        return name;
    }

}