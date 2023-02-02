package linkeddeque;

import java.util.Deque;

public class Helper {

    public static String stringRep(Deque<?> queue) {
        return switch (queue.size()) {
            case 0 -> "[ ]";
            case 1 -> "[ " + queue.peek() + " ]";
            case 2 -> "[ " + queue.peek() + " " + queue.peekLast() + " ]";
            default -> queue.toString();
        };
    }

}
