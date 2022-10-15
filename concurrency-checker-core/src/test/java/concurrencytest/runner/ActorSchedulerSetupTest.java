package concurrencytest.runner;

import concurrencytest.config.BasicConfiguration;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import concurrencytest.util.FileUtils;
import concurrencytest.v2.test.SimpleSharedCounter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ActorSchedulerSetupTest {

    private File mainFolder;

    @After
    public void cleanup() throws IOException {
        if (mainFolder != null) {
            FileUtils.deltree(mainFolder);
        }
    }

    @Test
    public void testPrepare() throws IOException {
        BasicConfiguration configuration = new BasicConfiguration(SimpleSharedCounter.class);
        ActorSchedulerSetup setup = new ActorSchedulerSetup(configuration);
        setup.prepare();
        this.mainFolder = configuration.outputFolder();
        File parentFolder = new File(mainFolder, SimpleSharedCounter.class.getPackageName().replace('.', '/'));
        Assert.assertTrue(parentFolder.exists());
        Assert.assertTrue(parentFolder.isDirectory());
        File[] files = parentFolder.listFiles((dir, name) -> name.endsWith(".class"));
        Assert.assertNotNull(files);
        Assert.assertEquals(1, files.length);
        File classFile = files[0];
        AtomicInteger checkpointCount = new AtomicInteger();
        try (FileInputStream fIn = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(fIn);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            if (owner.equals(Type.getInternalName(CheckpointRuntimeAccessor.class))) {
                                checkpointCount.incrementAndGet();
                            }
                        }
                    };
                }
            }, ClassReader.EXPAND_FRAMES);
        }
        Assert.assertEquals(8, checkpointCount.get());
    }
}