package concurrencytest.reflection;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public class StaticInitializer implements Member {

    private final Class<?> declaringClass;

    public StaticInitializer(Class<?> declaringClass) {
        this.declaringClass = declaringClass;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return "<cinit>";
    }

    @Override
    public int getModifiers() {
        return Modifier.STATIC | Modifier.PRIVATE;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
