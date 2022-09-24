package concurrencytest;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class LList<E> {

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public E element() {
        if (this.element == NullMarker.INSTANCE) {
            throw new NoSuchElementException();
        }
        return this.element;
    }

    private enum NullMarker {
        INSTANCE
    }

    public static final LList EMPTY = new LList(NullMarker.INSTANCE, null);

    public static <E> LList<E> empty() {
        return EMPTY;
    }

    private final E element;
    private final LList<E> next;

    private int hashCode;

    public LList(E element) {
        this(element, LList.empty());
    }

    public LList<E> prepend(E element) {
        return new LList<>(element, this);
    }

    public LList(E element, LList<E> next) {
        Objects.requireNonNull(element, "element cannot be null");
        this.element = element;
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LList<?> link = (LList<?>) o;
        return this.hashCode() == link.hashCode && Objects.equals(element, link.element) && Objects.equals(next, link.next);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(element, next);
        }
        return hashCode;
    }

    public List<E> reverse() {
        if (isEmpty()) {
            return new ArrayList<>();
        } else {
            var next = this.next.reverse();
            next.add(this.element);
            return next;
        }
    }

}
