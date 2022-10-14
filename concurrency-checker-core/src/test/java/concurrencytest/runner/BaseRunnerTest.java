package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.config.Configuration;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runtime.tree.ActorInformation;
import concurrencytest.runtime.tree.HeapTree;
import concurrencytest.util.ASMUtils;
import org.junit.Assert;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseRunnerTest {
    private final String suffix = "$$" + System.currentTimeMillis();

    private final Map<String, byte[]> classesByteCode = new HashMap<>();

    public SimpleRemapper getRemapper(Configuration configuration) {
        HashMap<String, String> classRenames = new HashMap<>();
        for (Class<?> c : configuration.classesToInstrument()) {
            String iName = Type.getInternalName(c);
            classRenames.put(iName, iName + suffix);
        }
        String iName = Type.getInternalName(configuration.mainTestClass());
        classRenames.put(iName, iName + suffix);
        return new SimpleRemapper(classRenames);
    }

    public void assertNotBlockedNoLocks(ActorInformation information) {
        Assert.assertFalse(information.isBlocked());
        Assert.assertTrue(information.monitorsOwned().isEmpty());
        Assert.assertTrue(information.locksLocked().isEmpty());
        Assert.assertEquals(Optional.empty(), information.waitingForLock());
        Assert.assertEquals(Optional.empty(), information.waitingForMonitor());
    }

    public ActorSchedulerEntryPoint prepare(Configuration configuration) {
        HeapTree tree = new HeapTree();
        CheckpointRegister register = new StandardCheckpointRegister();
        SimpleRemapper remapper = getRemapper(configuration);
        Map<String, byte[]> bytecodes = new HashMap<>(); // classname to bytes
        for (Class<?> cue : configuration.classesToInstrument()) {
            byte[] bytes = enhanceClass(cue, configuration, register, remapper);
            bytecodes.put(cue.getName(), bytes);
            String newName = remapper.map(Type.getInternalName(cue));
            if (newName != null) {
                bytecodes.put(Type.getObjectType(newName).getClassName(), bytes);
            }
        }
        byte[] bytes = enhanceClass(configuration.mainTestClass(), configuration, register, remapper);
        bytecodes.put(configuration.mainTestClass().getName(), bytes);
        String newInternalName = remapper.map(Type.getInternalName(configuration.mainTestClass()));
        String newClassName = Type.getObjectType(newInternalName).getClassName();
        bytecodes.put(newClassName, bytes);
        var cloader = new ClassLoader() {
            protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bc = bytecodes.get(name);
                if (bc == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bc, 0, bc.length);
            }
        };
        try {
            Class<?> mainTestClass = Class.forName(newClassName, true, cloader);
            return new ActorSchedulerEntryPoint(tree, register, configuration, mainTestClass, false);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] enhanceClass(Class<?> cue, Configuration configuration, CheckpointRegister register, SimpleRemapper remapper) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        var rem = new ClassRemapper(writer, remapper);
        ClassVisitor injectorVisitor = ActorSchedulerSetup.createCheckpointsVisitor(configuration, rem, register, ReflectionHelper.getInstance(), cue);
        try {
            ClassReader reader = ASMUtils.readClass(cue);
            Assert.assertNotNull(reader);
            reader.accept(injectorVisitor, ClassReader.EXPAND_FRAMES);
            CheckClassAdapter adapter = new CheckClassAdapter(null, true);
            byte[] byteCode = writer.toByteArray();
            reader = new ClassReader(byteCode);
            reader.accept(adapter, ClassReader.EXPAND_FRAMES);
            return byteCode;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
