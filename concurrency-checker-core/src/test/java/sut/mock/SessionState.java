package sut.mock;

public class SessionState {

    public static final SessionState DEFAULT = new SessionState(null);

    private final String id;

    public SessionState(String id) {
        this.id = id;
    }

    public void clear() {
        synchronized (this) {
            System.out.println("clearing");
        }
    }

}
