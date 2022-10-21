package sut.mock;

import concurrencytest.runtime.CheckpointRuntimeAccessor;

public class SessionManager {

    private final Session sharedSession = new Session();

    public void start() {
        SessionState state = new SessionState("brand new session");
        synchronized (state) {
            sharedSession.setSessionState(state);
            CheckpointRuntimeAccessor.manualCheckpoint();
            sharedSession.start();
        }
    }

    public void stop() {
        synchronized (sharedSession.getSessionState()) {
            sharedSession.stop();
        }
    }

}
