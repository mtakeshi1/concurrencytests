package concurrencytest.util;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;

public class ASMUtils {

    public static ClassReader readClass(Class<?> type) throws IOException {
        if (type.isArray() || type.isPrimitive() || type.getClassLoader() == null) {
            return null;
        }
        InputStream inputStream = type.getClassLoader().getResourceAsStream(type.getName().replace('.', '/') + ".class");
        if (inputStream == null) {
            throw new RuntimeException("Could not find class file for class: " + type);
        }
        return new ClassReader(inputStream);
    }

}
