package sut.mock;

import concurrencytest.TestRuntime;

public class SessionManager {

    private final Session sharedSession = new Session();

    public void start() {
        SessionState state = new SessionState("brand new session");
        synchronized (state) {
            sharedSession.setSessionState(state);
            TestRuntime.autoCheckpoint(this);
            sharedSession.start();
        }
    }

    public void stop() {
        synchronized (sharedSession.getSessionState()) {
            sharedSession.stop();
        }
    }

}
