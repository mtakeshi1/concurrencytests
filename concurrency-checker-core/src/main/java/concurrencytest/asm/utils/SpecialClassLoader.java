package concurrencytest.asm.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpecialClassLoader extends ClassLoader {

    private final File rootPath;

    private final Map<String, Class<?>> localLoadedClass = new HashMap<>();

    public SpecialClassLoader(ClassLoader parent, File rootPath) {
        super(parent);
        this.rootPath = rootPath;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (localLoadedClass.containsKey(name)) {
            return localLoadedClass.get(name);
        }
        File maybeClassFile = new File(rootPath, name.replace('.', '/') + ".class");
        if (maybeClassFile.exists()) {
            byte[] bytes = readFully(maybeClassFile);
            Class<?> custom = super.defineClass(name, bytes, 0, bytes.length);
            localLoadedClass.put(name, custom);
            return custom;
        }
        return super.loadClass(name, resolve);
    }

    public static byte[] readFully(File maybeClassFile) throws ClassNotFoundException {
        try (var din = new DataInputStream(new FileInputStream(maybeClassFile))) {
            byte[] data = new byte[(int) maybeClassFile.length()];
            din.readFully(data);
            return data;
        } catch (IOException e) {
            throw new ClassNotFoundException("IOError trying to read class file: " + maybeClassFile.getAbsolutePath(), e);
        }
    }

}
