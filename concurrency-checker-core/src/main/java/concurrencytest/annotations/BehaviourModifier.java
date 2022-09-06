package concurrencytest.annotations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum BehaviourModifier {

    STATIC, VOLATILE, TRANSIENT, SYNCHRONIZED, INSTANCE_MEMBER, FINAL, ABSTRACT, INTERFACE;

    public static List<BehaviourModifier> unreflect(int modifiers) {
        List<BehaviourModifier> list = new ArrayList<>();
        if (Modifier.isStatic(modifiers)) {
            list.add(STATIC);
        } else {
            list.add(INSTANCE_MEMBER);
        }
        if (Modifier.isVolatile(modifiers)) {
            list.add(VOLATILE);
        }
        if (Modifier.isTransient(modifiers)) {
            list.add(TRANSIENT);
        }
        if (Modifier.isSynchronized(modifiers)) {
            list.add(SYNCHRONIZED);
        }
        if (Modifier.isFinal(modifiers)) {
            list.add(FINAL);
        }

        if (Modifier.isAbstract(modifiers)) {
            list.add(ABSTRACT);
        }
        if (Modifier.isInterface(modifiers)) {
            list.add(INTERFACE);
        }
        return Collections.unmodifiableList(list);
    }
}
