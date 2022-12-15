package concurrencytest.runner;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Actors;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.asm.*;
import concurrencytest.asm.utils.ReadClassesVisitor;
import concurrencytest.asm.utils.SpecialClassLoader;
import concurrencytest.asm.utils.SpecialMethods;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.StandardCheckpointRegister;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.reflection.ClassResolver;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runner.statistics.GenericStatistics;
import concurrencytest.runner.statistics.ImmutableRunStatistics;
import concurrencytest.runner.statistics.MutableRunStatistics;
import concurrencytest.runner.statistics.RunStatistics;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.runtime.tree.TreeNode;
import concurrencytest.util.ASMUtils;
import concurrencytest.util.Utils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActorSchedulerSetup implements TaskSchedulerInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorSchedulerSetup.class);

    private final Configuration configuration;

    private final String suffix = "$$" + System.currentTimeMillis();

    private SimpleRemapper remapper;

    private final CheckpointRegister checkpointRegister = new StandardCheckpointRegister();

    private final List<Future<Optional<Throwable>>> futures = new CopyOnWriteArrayList<>();

    private final Deque<Collection<String>> pendingForks = new ConcurrentLinkedDeque<>();
    private MutableRunStatistics stat;

    public ActorSchedulerSetup(Configuration configuration) {
        this.configuration = configuration;
    }

    public static Map<String, Function<Object, Throwable>> parseActorMethods(Class<?> mainTestClass) {
        Map<String, Function<Object, Throwable>> map = new HashMap<>();
        ReflectionHelper.forEachAnnotatedMethod(Actor.class, mainTestClass, ((actor, m) -> {
            String actorName = baseActorName(m, actor);
            var old = map.put(actorName, adaptMethod(m, 0, actorName));
            if (old != null) {
                throw new IllegalArgumentException("Two methods have the same actor name '%s': %s and %s".formatted(actorName, old, m));
            }
        }));
        ReflectionHelper.forEachAnnotatedMethod(Actors.class, mainTestClass, ((actors, m) -> {
            for (int i = 0; i < actors.value().length; i++) {
                String actorName = baseActorName(m, actors.value()[i]) + "_" + i;
                var old = map.put(actorName, adaptMethod(m, i, actorName));
                if (old != null) {
                    throw new IllegalArgumentException("Two methods have the same actor name '%s': %s and %s".formatted(actorName, old, m));
                }
            }
        }));
        ReflectionHelper.forEachAnnotatedMethod(MultipleActors.class, mainTestClass, (ma, m) -> {
            for (int i = 0; i < ma.numberOfActors(); i++) {
                String actorName = (ma.actorPreffix().isEmpty() ? m.getName() : ma.actorPreffix()) + "_" + i;
                var old = map.put(actorName, adaptMethod(m, i, actorName));
                if (old != null) {
                    throw new IllegalArgumentException("Two methods have the same actor name '%s': %s and %s".formatted(actorName, old, m));
                }
            }
        });
        return map;
    }

    private static Function<Object, Throwable> adaptMethod(Method method, int actorIndex, String actorName) {
        try {
            MethodHandle base = MethodHandles.publicLookup().unreflect(method);
            for (int i = base.type().parameterCount() - 1; i > 0; i--) { //0 should be the invocation target
                if (base.type().parameterType(i) == int.class) {
                    base = MethodHandles.insertArguments(base, i, actorIndex);
                } else if (base.type().parameterType(i) == String.class) {
                    base = MethodHandles.insertArguments(base, i, actorName);
                } else {
                    throw new IllegalArgumentException("Cannot insert argument of type: %s to actor method: %s".formatted(base.type().parameterType(i), method));
                }
            }
            MethodHandle adapted = base;
            return new Function<>() {
                @Override
                public Throwable apply(Object o) {
                    try {
                        adapted.invoke(o);
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                }


                @Override
                public String toString() {
                    return "Actor '%s' for method: '%s'".formatted(actorName, method);
                }
            };
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("actor method: %s cannot be used due to IllegalAccessException".formatted(method), e);
        }
    }

    private static String baseActorName(Method m, Actor actor) {
        String actorName;
        if (actor.actorName().isEmpty()) {
            actorName = m.getName();
        } else {
            actorName = actor.actorName();
        }
        return actorName;
    }

    public Optional<Throwable> run(Consumer<TreeNode> treeObserver, Collection<? extends String> preselectedPath) throws IOException, ActorSchedulingException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, TimeoutException {
        File folder = configuration.outputFolder();
        if (!folder.isDirectory()) {
            throw new RuntimeException("%s is not a directory".formatted(folder.getAbsolutePath()));
        }
        ExecutionMode mode = prepare();
        if (mode == ExecutionMode.FORK) {
            return Utils.todo();
        } else {
            return runInVm(mode, configuration, checkpointRegister, treeObserver, preselectedPath);
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

    private static RunStatistics collectAndReset(MutableRunStatistics[] statistics) {
        RunStatistics combined = ImmutableRunStatistics.ZERO;
        for (var s : statistics) {
            if (s != null) {
                combined = combined.sumWith(s);
                s.reset(); // yes this is not 100% accurate but hopefully it will not be too bad
            }
        }
        return combined;
    }

    private static Runnable monitorTask(MutableRunStatistics statistics, List<Future<Optional<Throwable>>> futures, int parallel) {
        AtomicLong gcTime = new AtomicLong(GenericStatistics.totalGCTimeNanos());
        AtomicLong cpuTime = new AtomicLong(GenericStatistics.totalCPUUsageTimeNanos());
        AtomicLong totalRuns = new AtomicLong();
        AtomicLong lastTimeNanos = new AtomicLong(System.nanoTime());
        return new Runnable() {
            @Override
            public void run() {
                long runningThreads = Math.min(futures.stream().filter(f -> !f.isDone()).count(), parallel);
                double procs = Runtime.getRuntime().availableProcessors();
                MDC.put("actor", "MONITOR");
                RunStatistics collected = statistics.reset();
                long total = totalRuns.addAndGet(collected.numberOfRuns());
                long gc = GenericStatistics.totalGCTimeNanos() - gcTime.get();
                long cpu = GenericStatistics.totalCPUUsageTimeNanos() - cpuTime.get();
                double timeDiff = System.nanoTime() - lastTimeNanos.get(); // it should be almost impossible to be zero, but in a low resolution system maybe
                double cpuUsage = timeDiff < 0.1 ? 0 : (100.0 * cpu / procs / timeDiff);
                double gcUsage = timeDiff < 0.1 ? 0 : (100.0 * gc / procs / timeDiff);
                LOGGER.info("%s - total runs: %d, running threads: %d, cpu usage: %.2f gc: %.2f".formatted(collected.format(), total, runningThreads, cpuUsage, gcUsage));
                gcTime.set(GenericStatistics.totalGCTimeNanos());
                cpuTime.set(GenericStatistics.totalCPUUsageTimeNanos());
                lastTimeNanos.set(System.nanoTime());
                MDC.clear();
            }
        };
    }


    private Optional<Throwable> runInVm(ExecutionMode mode, Configuration configuration, CheckpointRegister register, Consumer<TreeNode> treeObserver, Collection<? extends String> preselectedPath)
            throws InterruptedException, IOException, ClassNotFoundException, TimeoutException {
        Tree tree = configuration.treeMode().instantiateTree();
        MDC.put("actor", "COORDINATOR");
        ExecutorService service = Executors.newFixedThreadPool(configuration.parallelExecutions());
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        this.stat = new MutableRunStatistics();
        AtomicReference<Throwable> errorHolder = new AtomicReference<>();
        Consumer<Throwable> errorReporter = t -> {
            synchronized (errorHolder) {
                cancelTasks(futures);
                Throwable throwable = errorHolder.get();
//                if (throwable != null && throwable != t) throwable.addSuppressed(t);
//                else
                if (throwable == null) errorHolder.set(t);
            }
        };
        Runnable command = monitorTask(stat, futures, configuration.parallelExecutions());
        scheduledExecutorService.scheduleWithFixedDelay(command, 60, 60, TimeUnit.SECONDS); //FIXME
        long timeout = System.nanoTime() + configuration.durationConfiguration().maxTotalDuration().toNanos();
        try {
            TaskSchedulerInterface taskSchedulerInterface = this;
            AtomicInteger actorIndex = new AtomicInteger();
            spawnFork(mode, configuration, register, treeObserver, tree, service, errorReporter, taskSchedulerInterface, actorIndex.getAndIncrement(), preselectedPath);
            while (tasksRunning() && System.nanoTime() < timeout) {
                waitForSignal();
                if (!pendingForks.isEmpty()) {
                    synchronized (this) {
                        while (!pendingForks.isEmpty()) {
                            var task = pendingForks.poll();
                            LOGGER.info("Starting fork: {}", task);
                            spawnFork(mode, configuration, register, treeObserver, tree, service, errorReporter, taskSchedulerInterface, actorIndex.getAndIncrement(), task);
                        }
                    }
                }
            }
            boolean taskRunning = false;
            for (var fut : futures) {
                try {
                    if (!fut.isDone()) {
                        fut.cancel(true);
                        taskRunning = true;
                    }
                    Optional<Throwable> optional = fut.get();
                    if (optional.isPresent()) {
                        cancelTasks(futures);
                        return optional;
                    }
                } catch (CancellationException e) {
                    //ignore
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof IOException ioe) {
                        throw ioe;
                    } else if (e.getCause() instanceof ClassNotFoundException cnfe) {
                        throw cnfe;
                    } else if (e.getCause() instanceof RuntimeException rte) {
                        throw rte;
                    } else {
                        throw new RuntimeException(e.getCause());
                    }
                }
            }
            if (taskRunning) {
                try {
                    cancelTasks(futures);
                } catch (Exception e) {
                    // ignore
                }
                throw new TimeoutException("max duration exceeded: " + configuration.durationConfiguration().maxTotalDuration());
            }
            command.run();
            return Optional.ofNullable(errorHolder.get());
        } finally {
            scheduledExecutorService.shutdown();
            service.shutdownNow();
        }
    }

    private void spawnFork(ExecutionMode mode, Configuration configuration, CheckpointRegister register, Consumer<TreeNode> treeObserver, Tree tree, ExecutorService service,
                           Consumer<Throwable> errorReporter, TaskSchedulerInterface taskSchedulerInterface, int actorIndex, Collection<? extends String> collection) {
        Callable<Optional<Throwable>> task = () -> {
            Class<?> mainTestClass = loadMainTestClass(mode);
            ActorSchedulerEntryPoint entryPoint = new ActorSchedulerEntryPoint(tree, register, configuration, new ArrayList<>(collection), mainTestClass, configuration.maxLoopIterations(),
                    "scheduler_" + actorIndex, errorReporter, taskSchedulerInterface);
            return entryPoint.exploreAll(treeObserver, stat);
        };
        futures.add(service.submit(task));
    }

    private synchronized void waitForSignal() throws InterruptedException {
        this.wait(1000);
    }

    private boolean tasksRunning() {
        return numberOfRunningTasks() > 0;
    }


    private void cancelTasks(List<Future<Optional<Throwable>>> futures) {
        for (var fut : futures) {
            fut.cancel(true);
        }
    }

    public static Collection<String> parseInitialActorNames(Class<?> mainTestClass) {
        return parseActorMethods(mainTestClass).keySet();
    }

    private Class<?> loadMainTestClass(ExecutionMode mode) throws MalformedURLException, ClassNotFoundException {
        if (mode == ExecutionMode.RENAMING) {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{this.configuration.outputFolder().toURI().toURL()});
            String renamedTestClass = remapper.map(Type.getInternalName(configuration.mainTestClass()));
            if (renamedTestClass == null) {
                renamedTestClass = configuration.mainTestClass().getName();
            }
            return Class.forName(Type.getObjectType(renamedTestClass).getClassName(), false, classLoader);
        } else if (mode == ExecutionMode.CLASSLOADER_ISOLATION) {
            ClassLoader classLoader = new SpecialClassLoader(this.getClass().getClassLoader(), this.configuration.outputFolder());
            return Class.forName(configuration.mainTestClass().getName(), false, classLoader);
        } else {
            throw new IllegalArgumentException("unknown execution mode: " + mode);
        }
    }

    private void saveCheckpointInformation(CheckpointRegister register) throws IOException {
        try (var oout = new ObjectOutputStream(new FileOutputStream(new File(configuration.outputFolder(), "checkpoints.ser")))) {
            oout.writeObject(register);
        }
        LOGGER.info("Checkpoints generated: %d".formatted(register.allCheckpoints().size()));
        if (LOGGER.isDebugEnabled()) {
            for (var cp : register.allCheckpoints().values()) {
                LOGGER.debug(cp.toString());
            }
        }

    }

    private Tree createFileTree() throws IOException {
        return Utils.todo();
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

    private ExecutionMode selectMode() {
        if (configuration.executionMode() != ExecutionMode.AUTO) {
            return configuration.executionMode();
        }
        for (var c : configuration.classesToInstrument()) {
            if (c.getName().startsWith("java.") || c.getName().startsWith("javax.") || c.getName().startsWith("sun.")) {
                return ExecutionMode.FORK;
            }
        }
        return ExecutionMode.CLASSLOADER_ISOLATION;
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
        for (var matcher : checkpointConfiguration.methodsCallsToInstrument()) {
            delegate = new MethodInvocationVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver, matcher);
        }
        if (configuration.checkpointConfiguration().includeStandardMethods()) {
            delegate = new MethodInvocationVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver, SpecialMethods.DEFAULT_SPECIAL_METHODS);
        }
        if (checkpointConfiguration.lockAcquisitionCheckpointEnabled()) {
            delegate = new LockCheckpointVisitor(delegate, checkpointRegister, classUnderEnhancement, classResolver);
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
                String map = Objects.requireNonNull(remapper.map(Type.getInternalName(c)), "could not find renamed class name for class: %s".formatted(c.getName()));
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
                if (!c.isMemberClass()) {
                    classRenames.put(iName, iName + suffix);
                } else if (c.getDeclaringClass() != null && (configuration.classesToInstrument().contains(c.getDeclaringClass()) || configuration.mainTestClass().equals(c.getDeclaringClass()))) {
                    classRenames.put(iName, iName);
                }
            }
            String iName = Type.getInternalName(configuration.mainTestClass());
            classRenames.put(iName, iName + suffix);
            this.remapper = new SimpleRemapper(classRenames);
        }
        return remapper;
    }

    @Override
    public int numberOfRunningTasks() {
        int c = 0;
        for (var fut : futures) {
            if (!fut.isDone()) {
                c++;
            }
        }
        return c + pendingForks.size();
    }

    @Override
    public int maxRunningTasks() {
        return this.configuration.parallelExecutions();
    }

    protected synchronized boolean addForkIfPossible(Collection<String> fork) {
        if (numberOfRunningTasks() < maxRunningTasks()) {
            pendingForks.add(fork);
            return true;
        }
        return false;
    }


    @Override
    public <E> E spawnTasks(Function<TaskSpawner, E> action) {
        return action.apply(this::addForkIfPossible);
    }

    @Override
    public synchronized void notifyTaskFinished() {
        notifyAll();
    }
}
