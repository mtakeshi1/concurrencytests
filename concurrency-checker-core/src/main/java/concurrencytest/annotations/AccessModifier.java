package concurrencytest.annotations;

import java.lang.reflect.Modifier;

public enum AccessModifier {

    PUBLIC, PROTECTED, DEFAULT, PRIVATE;

    public static AccessModifier unreflect(int modifiers) {
        if (Modifier.isPrivate(modifiers)) {
            return PRIVATE;
        }
        if (Modifier.isPublic(modifiers)) {
            return PUBLIC;
        }
        if (Modifier.isProtected(modifiers)) {
            return PROTECTED;
        }
        return DEFAULT;
    }

    public static AccessModifier[] all() {
        return new AccessModifier[]{PUBLIC, PROTECTED, DEFAULT, PRIVATE};
    }
}
