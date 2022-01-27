package concurrencytest.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DerivedKeyMap<K, V> {

    private final Function<V, K> keyExtractor;

    private Object[] values;
    private int size;

    public DerivedKeyMap<K, V> copy() {
        return new DerivedKeyMap<>(keyExtractor, size, values);
    }

    public boolean containsAll(Collection<? extends V> collection) {
        for (V value : collection) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }


    private static class ValueEntry<T> {
        private T value;
        private ValueEntry<T> next;

        public void forEach(Consumer<? super T> consumer) {
            consumer.accept(value);
            ValueEntry<T> n = next;
            while (n != null) {
                consumer.accept(n.value);
                n = n.next;
            }
        }

    }

    public DerivedKeyMap(Function<V, K> keyExtractor) {
        this(4, keyExtractor);
    }

    private DerivedKeyMap(Function<V, K> keyExtractor, int size, Object[] container) {
        this.keyExtractor = keyExtractor;
        this.values = new Object[container.length];
        this.size = size;
        System.arraycopy(container, 0, this.values, 0, container.length);
    }

    public DerivedKeyMap(int initialCapacity, Function<V, K> keyExtractor) {
        int actualCapacity = ceiling(initialCapacity);
        this.values = new Object[actualCapacity];
        this.keyExtractor = keyExtractor;
    }

    private enum Identity implements Function {
        INSTANCE;

        @Override
        public Object apply(Object o) {
            return o;
        }
    }

    public static <T> DerivedKeyMap<T, T> identityKey(int capacity) {
        return new DerivedKeyMap<>(capacity, Identity.INSTANCE);
    }

    public void forEachValue(Consumer<? super V> consumer) {
        for (Object o : values) {
            if (o != null) {
                consumer.accept((V) o);
            }
        }
    }

    public static <T> DerivedKeyMap<T, T> identityKey() {
        return identityKey(2);
    }

    public int capacity() {
        return values.length;
    }

    private static int ceiling(int initialCapacity) {
        for (int i = 4; i > 0; i <<= 1) {
            if (i > initialCapacity) {
                return initialCapacity;
            }
        }
        throw new IllegalArgumentException("Illegal capacity (too big)");
    }

    private void increaseCapacity() {
        int newSize = values.length << 1;
        if (newSize < 0) {
            throw new RuntimeException("cannot grow over: " + values.length);
        }
        Object[] newValues = new Object[newSize];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                K key = keyExtractor.apply((V) values[i]);
                int newIndex = key.hashCode() & (newSize - 1);
                insertInto(values[i], newValues, newIndex); //TODO
            }
        }
        values = newValues;
    }

    private static void insertInto(Object value, Object[] newValues, int baseIndex) {
        for (int i = baseIndex; i != baseIndex - 1; ) {
            if (newValues[i] == null) {
                newValues[i] = value;
                return;
            }
            if (++i == newValues.length) {
                i = 0;
            }
        }
    }


    public Collection<? extends V> values() {
        return valuesStream().collect(Collectors.toList());
    }

    public Optional<V> valueFor(K key) {
        int index = key.hashCode() & (values.length - 1);

        for (int i = index; i != index - 1; ) {
            if (values[i] == null) {
                return Optional.empty();
            }
            K existing = keyExtractor.apply((V) values[i]);
            if (existing.equals(key)) {
                return Optional.of((V) values[i]);
            }
            if (++i >= values.length) {
                i = 0;
            }
        }

        return null;
    }

    public boolean addNew(V value) {
        K key = keyExtractor.apply(value);
        int index = key.hashCode() & (values.length - 1);

        int last = index - 1;
        for (int i = index; i != last; ) {
            if (values[i] == null) {
                values[i] = value;
                if (shouldResize()) {
                    increaseCapacity();
                }
                return true;
            }
            K presentKey = keyExtractor.apply((V) values[i]);
            if (presentKey.equals(key)) {
                return false;
            }
            if (++i >= values.length) {
                i = 0;
            }
        }
        if (values[last] == null) {
            values[last] = value;
            if (shouldResize()) {
                increaseCapacity();
            }
            return true;
        }
        throw new RuntimeException("over capacity");
    }

    public Stream<V> valuesStream() {
        return Arrays.stream(values).filter(v -> v != null).map(v -> (V) v);
    }

    public Stream<K> keyStream() {
        return valuesStream().map(keyExtractor);
    }

    public void addAll(DerivedKeyMap<K, V> otherMap) {
        for (Object o : otherMap.values) {
            if (o != null) {
                this.addNew((V) o);
            }
        }
//        otherMap.valuesStream().forEach(s -> this.addNew(s));
    }

    public int size() {
        return size;
    }

    public boolean contains(V value) {
        return valueFor(keyExtractor.apply(value)).map(value::equals).orElse(false);
    }


    public V addOrMerge(V newValue, BiFunction<V, V, V> mergeFunction) {
        K key = keyExtractor.apply(newValue);
        int index = key.hashCode() & (values.length - 1);

        for (int i = index; i != index - 1; ) {
            if (values[i] == null) {
                values[i] = newValue;
                if (shouldResize()) {
                    increaseCapacity();
                }
                return newValue;
            }
            K presentKey = keyExtractor.apply((V) values[i]);
            if (presentKey.equals(key)) {
                values[i] = mergeFunction.apply(newValue, (V) values[i]);
                return (V) values[i];
            }
            if (++i >= values.length) {
                i = 0;
            }
        }
        throw new RuntimeException("over capacity");
    }

    protected boolean shouldResize() {
        return ++size >= (3 * (values.length / 4));
    }

    public Map<K, V> toMap(Supplier<Map<K, V>> factory) {
        Map<K, V> map = factory.get();
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                V value = (V) values[i];
                K key = keyExtractor.apply(value);
                map.put(key, value);
            }
        }
        return map;
    }

}
