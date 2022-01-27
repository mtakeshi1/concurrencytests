package sut.mock;

public class Session {

    private SessionState sessionState;

    private volatile boolean stopped = true;

    public Session() {
        sessionState = new SessionState(null);
    }

    public Session setSessionState(SessionState sessionState) {
        this.sessionState = sessionState;
        return this;
    }

    public SessionState getSessionState() {
        return sessionState;
    }


    public void start() {
        synchronized (this) {
            stopped = false;
        }
    }

    public void stop() {
        synchronized (this) {
            sessionState.clear();
            stopped = true;
        }
    }
}
