package concurrencytest.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper {

    private static final Map<String, Class<?>> PRIMITIVE_CLASS_NAMES;
    private static final Map<Class<?>, String> PRIMITIVE_INTERNAL_NAMES;

    private static void populate(Class<?> c, Map<String, Class<?>> m) {
        m.put(c.getName(), c);
    }

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
}
