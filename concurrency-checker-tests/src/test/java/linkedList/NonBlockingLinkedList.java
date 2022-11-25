package linkedList;

import concurrencytest.runtime.CheckpointRuntimeAccessor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class NonBlockingLinkedList<T> {

    public static record Node<T>(T value, AtomicReference<Node<T>> next) {

        public Node(T value, Node<T> nextNode) {
            this(value, new AtomicReference<>(nextNode));
        }

        public Node(T value) {
            this(value, new AtomicReference<>());
        }

        public Node {
            Objects.requireNonNull(value);
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    public void prepend(T t) {
        Node<T> previousHead = head.get();
        Node<T> nextHead = new Node<>(t, previousHead);
        while (!head.compareAndSet(previousHead, nextHead)) {
            previousHead = head.get();
            CheckpointRuntimeAccessor.manualCheckpoint();
            nextHead = new Node<>(t, previousHead);
        }
    }

    public T removeFirst() {
        Node<T> previousHead = head.get();
        if (previousHead == null) {
            return null;
        }
        Node<T> nextHead = previousHead.next().get();
        while (!head.compareAndSet(previousHead, nextHead)) {
            previousHead = head.get();
            CheckpointRuntimeAccessor.manualCheckpoint();
            if (previousHead == null) {
                return null;
            }
            nextHead = previousHead.next().get();
        }
        return previousHead.value;
    }


    public int size() {
        Node<T> previousHead = head.get();
        if (previousHead == null) {
            return 0;
        }
        int c = 0;
        while (previousHead != null) {
            previousHead = previousHead.next().get();
            c++;
        }
        return c;
    }

}
