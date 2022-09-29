package concurrencytest.runner;

import java.util.Collection;
import java.util.List;

public class NoRunnableActorFoundException extends ActorSchedulingException {
    private final Collection<String> unexploredPathNodes;
    private final List<String> runnableActors;

    public NoRunnableActorFoundException(Collection<String> unexploredPathNodes, List<String> runnableActors) {
        this.unexploredPathNodes = unexploredPathNodes;
        this.runnableActors = runnableActors;
    }

    public Collection<String> getUnexploredPathNodes() {
        return unexploredPathNodes;
    }

    public List<String> getRunnableActors() {
        return runnableActors;
    }
}
