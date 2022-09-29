package concurrencytest.runner;

import concurrencytest.asm.*;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runtime.tree.HeapTree;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.util.ASMUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Function;

public class ActorSchedulerSetup {

    private final Configuration configuration;

    private final String suffix = "$$" + System.currentTimeMillis();

    private SimpleRemapper remapper;

    private CheckpointRegister checkpointRegister = new StandardCheckpointRegister();

    public ActorSchedulerSetup(Configuration configuration) {
        this.configuration = configuration;
    }

    public void initialize() throws IOException {
        File folder = configuration.outputFolder();
        if (!folder.isDirectory()) {
            throw new RuntimeException("%s is not a directory".formatted(folder.getAbsolutePath()));
        }
        saveConfiguration();
        ExecutionMode mode = selectMode();
        CheckpointRegister register = createRegister();
        enhanceClasses(mode, folder, register, ReflectionHelper.getInstance());
        renameMainTestClassIfNecessary(mode, folder);
        saveCheckpointInformation(register);
        Tree tree;
        if (mode == ExecutionMode.FORK) {
            tree = createFileTree();
            throw new RuntimeException("not yet implemented");
        } else {
            tree = new HeapTree();
        }
    }

    private void saveCheckpointInformation(CheckpointRegister register) {

    }

    private Tree createFileTree() throws IOException {
        throw new RuntimeException("not yet implemented");
    }

    private void saveConfiguration() throws IOException {
        try (var oout = new ObjectOutputStream(new FileOutputStream(new File(configuration.outputFolder(), "configuration.ser")))) {
            oout.writeObject(this.configuration);
        }
    }

    private void renameMainTestClassIfNecessary(ExecutionMode mode, File folder) throws IOException {
        ClassVisitor visitor = createDelegateFactory(mode, folder).apply(configuration.mainTestClass());
        ClassReader reader = ASMUtils.readClass(configuration.mainTestClass());
        if (reader == null) {
            throw new RuntimeException("Could not find classFile for class: %s".formatted(configuration.mainTestClass().getName()));
        }
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
    }

    private CheckpointRegister createRegister() {
        return checkpointRegister;
    }

    private ExecutionMode selectMode() {
        if (configuration.executionMode() != ExecutionMode.AUTO) {
            return configuration.executionMode();
        }
        return ExecutionMode.FORK;
    }

    private ClassVisitor createCheckpointsVisitor(ClassVisitor delegate, CheckpointRegister checkpointRegister, ClassResolver classResolver, Class<?> classUnderEnhancement) {
        if (configuration.checkClassesBytecode()) {
            delegate = new CheckClassAdapter(delegate, true);
        }
        CheckpointConfiguration checkpointConfiguration = configuration.checkpointConfiguration();
        if (checkpointConfiguration.manualCheckpointsEnabled()) {
            delegate = new ManualCheckpointVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        if (checkpointConfiguration.enhanceWaitParkNotify()) {
            delegate = new WaitParkWakeupVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        if (checkpointConfiguration.threadsEnhanced()) {
            delegate = new ThreadConstrutorVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        if (checkpointConfiguration.removeSynchronizedMethodDeclaration()) {
            delegate = new SynchronizedMethodDeclarationVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        if (checkpointConfiguration.monitorCheckpointEnabled()) {
            delegate = new SynchronizedMethodDeclarationVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        for (var matcher : checkpointConfiguration.fieldsToInstrument()) {
            delegate = new FieldCheckpointVisitor(matcher, delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        for (var matcher : checkpointConfiguration.arrayCheckpoints()) {
            delegate = new ArrayElementVisitor(
                    delegate, checkpointRegister, classUnderEnhancement, classResolver, matcher
            );
        }
        for (var matcher : checkpointConfiguration.methodsToInstrument()) {
            delegate = new MethodInvocationVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver, matcher);
        }
        return delegate;
    }

    private void enhanceClasses(ExecutionMode mode, File folder, CheckpointRegister register, ClassResolver classResolver) throws IOException {
        Function<Class<?>, ClassVisitor> delegateFactory = createDelegateFactory(mode, folder);
        for (Class<?> cue : configuration.classesToInstrument()) {
            ClassVisitor classVisitor = createCheckpointsVisitor(delegateFactory.apply(cue), register, classResolver, cue);
            ClassReader reader = ASMUtils.readClass(cue);
            if (reader == null) {
                throw new RuntimeException("Could not find classFile for class: %s".formatted(cue.getName()));
            }
            reader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        }

    }

    private Function<Class<?>, ClassVisitor> createDelegateFactory(ExecutionMode mode, File rootFolder) {
        if (mode == ExecutionMode.RENAMING) {
            return c -> {
                SimpleRemapper remapper = getRemapper();
                String map = remapper.map(Type.getInternalName(c));
                if (map == null) {
                    throw new RuntimeException("could not find renamed class name for class: %s".formatted(c.getName()));
                }
                String classFileName = map + ".class";
                return new ClassRemapper(fileWritingVisitor(rootFolder, classFileName), remapper);
            };
        } else {
            return c -> new ClassRemapper(fileWritingVisitor(rootFolder, Type.getInternalName(c) + ".class"), remapper);
        }
    }

    private ClassVisitor fileWritingVisitor(File rootFolder, String classFileName) {
        File destination = new File(rootFolder, classFileName);
        try {
            Files.createDirectories(destination.getParentFile().toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        return new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                byte[] bytes = writer.toByteArray();
                try (FileOutputStream fout = new FileOutputStream(destination)) {
                    fout.write(bytes);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    public SimpleRemapper getRemapper() {
        if (this.remapper == null) {
            HashMap<String, String> classRenames = new HashMap<>();
            for (Class<?> c : configuration.classesToInstrument()) {
                String iName = Type.getInternalName(c);
                classRenames.put(iName, iName + suffix);
            }
            this.remapper = new SimpleRemapper(classRenames);
        }
        return remapper;
    }

}
