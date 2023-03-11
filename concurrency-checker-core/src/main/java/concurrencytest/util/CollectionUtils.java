package concurrencytest.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<T> removeFirst(List<T> original, Predicate<T> predicate) {
        List<T> copy = new ArrayList<>(original.size());
        boolean removed = false;
        for (T element : original) {
            if (!removed && predicate.test(element)) {
                removed = true;
                continue;
            }
            copy.add(element);
        }
        return copy;
    }

    public static <T> List<T> copyAndAdd(List<T> list, T element) {
        List<T> copy = new ArrayList<>(list);
        copy.add(element);
        return copy;
    }

    public static <T> Collection<? extends T> nonNull(Collection<? extends T> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }
}
