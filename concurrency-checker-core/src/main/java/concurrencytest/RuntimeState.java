package concurrencytest;

import concurrencytest.util.DerivedKeyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RuntimeState {

    private final DerivedKeyMap<String, ThreadState> currentThreadStates;

    public RuntimeState(Map<String, ThreadState> currentThreadStates) {
        this.currentThreadStates = new DerivedKeyMap<>(ThreadState::getActorIdentification);
        currentThreadStates.values().forEach(this.currentThreadStates::addNew);
    }

    public String[] threadNames() {
        return currentThreadStates.keyStream().toArray(String[]::new);
    }

    public Map<String, ThreadState> getCurrentState() {
        return currentThreadStates.toMap(HashMap::new);
    }

    @Override
    public String toString() {
        return "RuntimeState{" +
                "currentThreadStates=" + getCurrentState() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuntimeState state = (RuntimeState) o;
        return Objects.equals(getCurrentState(), state.getCurrentState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCurrentState());
    }
}
