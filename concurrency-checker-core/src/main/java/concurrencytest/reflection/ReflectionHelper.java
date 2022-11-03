package concurrencytest.reflection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class ReflectionHelper implements ClassResolver {

    public static final int BRIDGE    = 0x00000040;
    public static final int VARARGS   = 0x00000080;
    public static final int SYNTHETIC = 0x00001000;
    public static final int ANNOTATION  = 0x00002000;
    public static final int ENUM      = 0x00004000;
    public static final int MANDATED  = 0x00008000;

    private static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES;
    private static final Map<Class<?>, String> PRIMITIVE_INTERNAL_NAMES;

    private static final Map<Integer, String> OPCODE_NAMES;

    private static void populate(Class<?> c, Map<String, Class<?>> m) {
        m.put(c.getName(), c);
    }

    private static final ReflectionHelper INSTANCE = new ReflectionHelper();

    static {
        HashMap<String, Class<?>> m = new HashMap<>();
        populate(boolean.class, m);
        populate(byte.class, m);
        populate(short.class, m);
        populate(int.class, m);
        populate(long.class, m);
        populate(char.class, m);
        populate(float.class, m);
        populate(double.class, m);
        populate(void.class, m);

        HashMap<Class<?>, String> n = new HashMap<>();
        n.put(boolean.class, "Z");
        n.put(byte.class, "B");
        n.put(short.class, "S");
        n.put(int.class, "I");
        n.put(long.class, "J");
        n.put(char.class, "C");
        n.put(float.class, "F");
        n.put(double.class, "D");
        n.put(void.class, "V");
        PRIMITIVE_CLASS_NAMES = Collections.unmodifiableMap(m);
        PRIMITIVE_INTERNAL_NAMES = Collections.unmodifiableMap(n);
        Map<Integer, String> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(ReflectionHelper.class.getResourceAsStream("/opcodes.properties"), "opcodes.properties file not found?")))) {
            reader.lines().map(s -> s.split("=")).forEach(arr -> map.put(Integer.parseInt(arr[0]), arr[1]));
        } catch (IOException | UncheckedIOException e) {
            throw new ExceptionInInitializerError(e);
        }
        OPCODE_NAMES = Collections.unmodifiableMap(map);
    }

    public static Class<?> resolveType(String className) throws ClassNotFoundException {
        Class<?> prim = PRIMITIVE_CLASS_NAMES.get(className);
        if (prim != null) {
            return prim;
        }
        if (className.endsWith("[]")) {
            String arrayTypeName = className.substring(0, className.length() - 2);
            Class<?> arrayType = resolveType(arrayTypeName);
            return Array.newInstance(arrayType, 0).getClass();
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return Class.forName(className, false, contextClassLoader);
            } catch (ClassNotFoundException cnfe) {
                //ignored
            }
        }
        return Class.forName(className);
    }

    public static boolean isPrimitive(String className) {
        return PRIMITIVE_CLASS_NAMES.containsKey(className);
    }

    public static boolean isPrimitiveWrapper(Class<?> type) {
        return type == Void.class || type == Boolean.class || type == Character.class || type == Byte.class || type == Short.class || type == Integer.class || type == Long.class || type == Float.class || type == Double.class;
    }

    public static boolean isUnmodifiableCollection(Class<?> type) {
        return type.getName().startsWith("java.util.Collections$Unmodifiable") && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
    }

    public static String renderOpcode(int opcode) {
        return OPCODE_NAMES.get(opcode);
    }

    public static ClassResolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<?> resolveName(String className) {
        try {
            return resolveType(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Method lookupMethod(Class<?> declaringClass, int access, String name, Class<?>[] args, Class<?> returnType) {
        for (Method m : declaringClass.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getReturnType().equals(returnType) && java.util.Arrays.equals(m.getParameterTypes(), args)) {
                return m;
            }
        }
        throw new RuntimeException(String.format("Could not find methodOrConstructor with name %s, parameter types: %s and return type: %s on class: %s", name, Arrays.toString(args), returnType, declaringClass));
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> Constructor<T> lookupConstructor(Class<T> declaringClass, Class<?>[] args) {
        for (Constructor<?> ctor : declaringClass.getConstructors()) {
            if (Arrays.equals(ctor.getParameterTypes(), args)) {
                return (Constructor<T>) ctor;
            }
        }
        return null;
    }

    @Override
    public Field lookupField(Class<?> ownerType, String name) {
        for (Field f : ownerType.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        throw new RuntimeException(String.format("Could not find field %s on type: %s", name, ownerType.getName()));
    }

    public static <A extends Annotation> void forEachAnnotatedMethod(Class<A> annotationType, Class<?> hostClass, BiConsumer<A, Method> action) {
        for(var m : hostClass.getMethods()) {
            doWithAnnotatedMethod(annotationType, m, action);
        }
    }


    public static <A extends Annotation> void doWithAnnotatedMethod(Class<A> annotationType, Method method, BiConsumer<A, Method> action) {
        A annotation = method.getAnnotation(annotationType);
        if (annotation != null) {
            action.accept(annotation, method);
        }
    }

}
