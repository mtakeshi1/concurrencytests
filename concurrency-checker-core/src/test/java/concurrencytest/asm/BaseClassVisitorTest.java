package concurrencytest.asm;

import concurrencytest.CheckpointRuntimeAccessor;
import concurrencytest.agent.OpenClassLoader;
import concurrencytest.asm.testClasses.InjectionTarget;
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
import java.util.function.Consumer;

public class BaseClassVisitorTest {

    private static int idSeed;

    public <E> RecordingCheckpointRuntime execute(E instance, Consumer<E> execution) {
        RecordingCheckpointRuntime runtime = new RecordingCheckpointRuntime(register);
        CheckpointRuntimeAccessor.setup(runtime);
        execution.accept(instance);
        return runtime;
    }

    protected CheckpointRegister register = new StandardCheckpointRegister();

    public Class<?> prepare(Class<?> target, VisitorBuilderFunction factory) throws IOException, ClassNotFoundException {
        ClassReader reader = ASMUtils.readClass(target);
        Assert.assertNotNull(reader);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String newName = target.getName() + "$$Injected_" + idSeed++;
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
        Class<?> prepared = prepare(InjectionTarget.class, (t, cv) -> cv);
        Runnable run = (Runnable) prepared.getConstructor().newInstance();
        run.run();
    }
}