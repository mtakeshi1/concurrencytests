package concurrencytest.agent;

import com.sun.tools.attach.VirtualMachine;
import concurrencytest.ConcurrencyRunner;
import concurrencytest.annotations.CheckpointInjectionPoint;
import concurrencytest.annotations.TestParameters;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public class Agent {
    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    public static void premain(String args, Instrumentation instrumentation) {
        Set<String> classInternalNames = new HashSet<>(Arrays.asList(args.split(",")));
        String expectedName = ConcurrencyRunner.class.getName();
        HashSet<String> unresolvedClassNames = new HashSet<>();
        List<Class> callbacks = Arrays.stream(instrumentation.getAllLoadedClasses()).filter(c -> c.getName().equals(expectedName)).collect(Collectors.toList());
        AtomicLong idGenerator = new AtomicLong();
        Class[] classes = Arrays.stream(instrumentation.getAllLoadedClasses()).filter(instrumentation::isModifiableClass).filter(c -> classInternalNames.contains(Type.getInternalName(c))).toArray(Class[]::new);
        Collection<CheckpointInjectionPoint> injectionPoints = collectInjectionPoints(classes);
        try {
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                    if (classInternalNames.contains(className)) {
                        return injectCheckpoints(className, loader, classfileBuffer, idGenerator::incrementAndGet, unresolvedClassNames, injectionPoints);
                    }
                    return null;

                }

            }, true);

            instrumentation.retransformClasses(classes);
            instrumentation.redefineClasses();
//            instrumentation.redefineClasses();
            System.out.println(idGenerator.get() + " checkpoints injected on " + classes.length + " classes instrumented");
        } catch (Throwable t) {
            for (Class agentClass : callbacks) {
                invokeAgentDone(agentClass, t);
            }
        } finally {
            for (Class agentClass : callbacks) {
                invokeAgentDone(agentClass, null);
            }
        }
    }

    private static Collection<CheckpointInjectionPoint> collectInjectionPoints(Class[] classes) {
        Set<CheckpointInjectionPoint> points = EnumSet.noneOf(CheckpointInjectionPoint.class);
        try {
            for (Class type : classes) {
                Annotation[] annotations = type.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().getName().equals(TestParameters.class.getName())) {
                        Object injectionPoints = annotation.annotationType().getDeclaredMethod("injectionPoints").invoke(annotation);
                        for (int i = 0; i < Array.getLength(injectionPoints); i++) {
                            Object point = Array.get(injectionPoints, i);
                            String name = (String) point.getClass().getMethod("name").invoke(point);
                            points.add(CheckpointInjectionPoint.valueOf(name));
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return points;
    }

    private static void invokeAgentDone(Class type, Throwable t) {
        try {
            Method method = type.getDeclaredMethod("agentDone", Throwable.class);
            method.invoke(null, new Object[]{null});
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static byte[] injectCheckpoints(String className, ClassLoader loader, byte[] classfileBuffer, LongSupplier idGenerator, Set<String> unresolvedClassNames, Collection<CheckpointInjectionPoint> injectionPoints) {
        try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassReader reader = new ClassReader(classfileBuffer);
            CheckClassAdapter adapter = new CheckClassAdapter(writer, false);
            InjectCheckpointVisitor classVisitor = new InjectCheckpointVisitor(adapter, Type.getObjectType(className).getClassName(), idGenerator, new HashMap<>(), unresolvedClassNames, injectionPoints);
            reader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (TypeNotPresentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void attachAndInstrument(Set<Class<?>> allClasses) {
        try {
            VirtualMachine machine = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
            String path = findSelfJarPath();
            machine.loadAgent(path, allClasses.stream().map(Type::getInternalName).collect(Collectors.joining(",")));
        } catch (Exception e) {
            ConcurrencyRunner.agentError = e;
        } finally {
            ConcurrencyRunner.agentDone = true;
        }
    }

    private static String findSelfJarPath() {
        URL baseURL = Agent.class.getResource("/" + Agent.class.getName().replace('.', '/') + ".class");
        if (baseURL != null && baseURL.getProtocol().equals("jar")) {
            String path = baseURL.getPath();
            if (path.startsWith("file:") && path.contains(".jar!")) {
                int last = path.indexOf(".jar!");
                return path.substring(5, last + 4);
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        VirtualMachine machine = VirtualMachine.attach(args[0]);
        String path = findSelfJarPath();
        machine.loadAgent(path, args[1]);
    }


}
