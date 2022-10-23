package concurrencytest.runner;

import concurrencytest.asm.*;
import concurrencytest.asm.utils.ReadClassesVisitor;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runtime.tree.HeapTree;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.util.ASMUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActorSchedulerSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSchedulerSetup.class);

    private final Configuration configuration;

    private final String suffix = "$$" + System.currentTimeMillis();

    private SimpleRemapper remapper;

    private final CheckpointRegister checkpointRegister = new StandardCheckpointRegister();

    public ActorSchedulerSetup(Configuration configuration) {
        this.configuration = configuration;
    }

    public Optional<Throwable> run(Consumer<TreeNode> treeObserver, Collection<? extends String> preselectedPath) throws IOException, ActorSchedulingException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        File folder = configuration.outputFolder();
        if (!folder.isDirectory()) {
            throw new RuntimeException("%s is not a directory".formatted(folder.getAbsolutePath()));
        }
        ExecutionMode mode = prepare();
        if (mode == ExecutionMode.FORK) {
            throw new RuntimeException("not yet implemented");
        } else {
            return runInVm(configuration, checkpointRegister, treeObserver, preselectedPath);
        }
    }

    public ExecutionMode prepare() throws IOException {
        File folder = configuration.outputFolder();
        saveConfiguration();
        ExecutionMode mode = selectMode();
        enhanceClasses(mode, folder, checkpointRegister, ReflectionHelper.getInstance());
        renameMainTestClassIfNecessary(mode, folder);
        saveCheckpointInformation(checkpointRegister);
        return mode;
    }

    private Optional<Throwable> runInVm(Configuration configuration, CheckpointRegister register, Consumer<TreeNode> treeObserver, Collection<? extends String> preselectedPath) throws ActorSchedulingException, InterruptedException, IOException, ClassNotFoundException {
        Tree tree = new HeapTree();
        Class<?> mainTestClass = loadMainTestClass();
        ActorSchedulerEntryPoint entryPoint = new ActorSchedulerEntryPoint(tree, register, configuration.durationConfiguration(), new ArrayList<>(preselectedPath), mainTestClass, configuration.maxLoopIterations());
        return entryPoint.exploreAll(treeObserver);
    }

    private Class<?> loadMainTestClass() throws MalformedURLException, ClassNotFoundException {
        URLClassLoader classLoader = new URLClassLoader(new URL[]{this.configuration.outputFolder().toURI().toURL()});
        String renamedTestClass = remapper.map(Type.getInternalName(configuration.mainTestClass()));
        if (renamedTestClass == null) {
            renamedTestClass = configuration.mainTestClass().getName();
        }
        return Class.forName(Type.getObjectType(renamedTestClass).getClassName(), false, classLoader);
    }

    private void saveCheckpointInformation(CheckpointRegister register) throws IOException {
        try (var oout = new ObjectOutputStream(new FileOutputStream(new File(configuration.outputFolder(), "checkpoints.ser")))) {
            oout.writeObject(register);
        }
        LOGGER.debug("Checkpoints generated: %d".formatted(register.allCheckpoints().size()));
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
        if (!configuration.classesToInstrument().contains(configuration.mainTestClass())) {
            ClassVisitor visitor = createCheckpointsVisitor(this.configuration, createDelegateFactory(mode, folder).apply(configuration.mainTestClass()), checkpointRegister, ReflectionHelper.getInstance(), configuration.mainTestClass());
            ClassReader reader = ASMUtils.readClass(configuration.mainTestClass());
            if (reader == null) {
                throw new RuntimeException("Could not find classFile for class: %s".formatted(configuration.mainTestClass().getName()));
            }
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        }
    }

    private CheckpointRegister getRegister() {
        return checkpointRegister;
    }

    private ExecutionMode selectMode() throws IOException {
        if (configuration.executionMode() != ExecutionMode.AUTO) {
            return configuration.executionMode();
        }
        Set<Class<?>> set = new HashSet<>(configuration.classesToInstrument());
        set.add(configuration.mainTestClass());
        if (isSelfContained(set)) {
//        if (configuration.classesToInstrument().size() == 1 && configuration.classesToInstrument().contains(configuration.mainTestClass())) {
            return ExecutionMode.RENAMING;
        }
        return ExecutionMode.FORK;
    }

    public static boolean isSelfContained(Collection<? extends Class<?>> toInstrument) throws IOException {
        for (Class<?> toRename : toInstrument) {
            Collection<? extends Class<?>> dependencies = findAllDependencies(toRename);
            for (Class<?> dep : dependencies) {
                for (Class<?> secondLevelDep : findAllDependencies(dep)) {
                    if (toInstrument.contains(secondLevelDep) && !toInstrument.contains(dep)) {
                        return false;
                    }
                }
            }
        }
        //TODO find a way to find out if renaming all of these classes would be enough
        return true;
    }

    private static Collection<? extends Class<?>> findDirectDependencies(Class<?> type) throws IOException {
        ClassReader reader = ASMUtils.readClass(type);
        if (reader == null) {
            return Collections.emptyList();
        }
        ReadClassesVisitor visitor = new ReadClassesVisitor();
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return visitor.getDiscoveredClasses();
    }

    public static Collection<? extends Class<?>> findAllDependencies(Class<?> type) throws IOException {
        Set<Class<?>> known = new HashSet<>();
        Queue<Class<?>> toVisit = new ArrayDeque<>();
        toVisit.add(type);
        while (!toVisit.isEmpty()) {
            Class<?> next = toVisit.poll();
            ClassReader reader = ASMUtils.readClass(next);
            if (reader == null) {
                known.add(next);
                continue;
            }
            ReadClassesVisitor visitor = new ReadClassesVisitor(new HashSet<>(known));
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            Set<Class<?>> latestDiscovered = visitor.getDiscoveredClasses();
            for (Class<?> maybeNew : latestDiscovered) {
                if (!known.contains(maybeNew)) {
                    toVisit.add(maybeNew);
                }
            }
            known.add(next);
        }
        return known;
    }

    public static ClassVisitor createCheckpointsVisitor(Configuration configuration, ClassVisitor delegate, CheckpointRegister checkpointRegister, ClassResolver classResolver, Class<?> classUnderEnhancement) {
//        if (configuration.checkClassesBytecode()) {
//            delegate = new CheckClassAdapter(delegate, true);
//        }
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
        if (checkpointConfiguration.monitorCheckpointEnabled()) {
            delegate = new SynchronizedBlockVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
        }
        if (checkpointConfiguration.removeSynchronizedMethodDeclaration()) {
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
        Collection<Class<?>> classes = configuration.classesToInstrument();
        Function<Class<?>, ClassVisitor> delegateFactory = createDelegateFactory(mode, folder);
        for (Class<?> cue : classes) {
            ClassVisitor classVisitor = createCheckpointsVisitor(this.configuration, delegateFactory.apply(cue), register, classResolver, cue);
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
            return c -> fileWritingVisitor(rootFolder, Type.getInternalName(c) + ".class");
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
                if (configuration.checkClassesBytecode()) {
                    ClassReader reader = new ClassReader(bytes);
                    reader.accept(new CheckClassAdapter(null, true), ClassReader.EXPAND_FRAMES);
                }
            }
        };
    }

    public SimpleRemapper getRemapper() {
        if (this.remapper == null) {
            HashMap<String, String> classRenames = new HashMap<>();
            for (Class<?> c : configuration.classesToInstrument()) {
                String iName = Type.getInternalName(c);
                if (!c.isMemberClass() && configuration.classesToInstrument().contains(c.getDeclaringClass())) {
                    classRenames.put(iName, iName + suffix);
                } else {
                    classRenames.put(iName, iName);
                }
            }
            String iName = Type.getInternalName(configuration.mainTestClass());
            classRenames.put(iName, iName + suffix);
            this.remapper = new SimpleRemapper(classRenames);
        }
        return remapper;
    }

}
