package concurrencytest.asm;

import concurrencytest.reflection.ReflectionHelper;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum BehaviourModifier {

    STATIC(Modifier.STATIC), VOLATILE(Modifier.VOLATILE), TRANSIENT(Modifier.TRANSIENT), SYNCHRONIZED(Modifier.SYNCHRONIZED), INSTANCE_MEMBER(0), FINAL(Modifier.FINAL),
    ABSTRACT(Modifier.ABSTRACT), INTERFACE(Modifier.INTERFACE), SYNTHETIC(ReflectionHelper.SYNTHETIC), BRIDGE(ReflectionHelper.BRIDGE);

    BehaviourModifier(int modifier) {
        this.modifier = modifier;
    }

    private final int modifier;

    public static Collection<BehaviourModifier> unreflect(int modifiers) {
        Set<BehaviourModifier> list = EnumSet.noneOf(BehaviourModifier.class);
        for (BehaviourModifier mod : values()) {
            if ((modifiers & mod.modifier) != 0) {
                list.add(mod);
            }
        }
        if (!list.contains(STATIC)) {
            list.add(INSTANCE_MEMBER);
        }
        return Collections.unmodifiableSet(list);
    }

    public int modifier() {
        return modifier;
    }
}
