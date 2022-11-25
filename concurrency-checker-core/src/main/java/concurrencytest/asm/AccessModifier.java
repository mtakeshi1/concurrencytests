package concurrencytest.asm;

import java.lang.reflect.Modifier;

/**
 * Enum to mirror java level access modifiers - and not mix up with other type of modifiers, like abstract, static, etc
 */
public enum AccessModifier {

    PUBLIC(Modifier.PUBLIC), PROTECTED(Modifier.PROTECTED), DEFAULT(0), PRIVATE(Modifier.PRIVATE);

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

    private final int modifier;

    AccessModifier(int modifier) {
        this.modifier = modifier;
    }

    public int modifier() {
        return modifier;
    }

    public static AccessModifier[] all() {
        return new AccessModifier[]{PUBLIC, PROTECTED, DEFAULT, PRIVATE};
    }
}
