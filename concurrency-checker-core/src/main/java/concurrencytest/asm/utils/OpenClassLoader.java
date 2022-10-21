package concurrencytest.asm.utils;

import java.util.HashMap;
import java.util.Map;

public class OpenClassLoader extends ClassLoader {

    private final Map<String, byte[]> userDefinedClasses = new HashMap<>();

    private final Map<String, Class<?>> loadedClasses = new HashMap<>();

    public synchronized void addClass(String className, byte[] bytecode) {
        userDefinedClasses.put(className, bytecode);
    }

    @Override
    protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> alreadyLoaded = loadedClasses.get(name);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        byte[] userDefined = userDefinedClasses.get(name);
        if (userDefined != null) {
            Class<?> t = defineClass(name, userDefined, 0, userDefined.length);
            loadedClasses.put(name, t);
            return t;
        }
        return super.findClass(name);
    }

}
