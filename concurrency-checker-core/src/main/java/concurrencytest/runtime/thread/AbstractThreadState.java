package concurrencytest.runtime.thread;

import concurrencytest.runtime.lock.BlockingResource;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractThreadState implements ThreadState {

    private final String actorName;
    private final int checkpoint;
    private final int loopCount;
    private final List<BlockingResource> ownedResources;

    public AbstractThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources) {
        this.actorName = Objects.requireNonNull(actorName);
        if (actorName.length() > MAX_ACTOR_NAME_LENGTH) {
            throw new IllegalArgumentException("maximum actor name length " + MAX_ACTOR_NAME_LENGTH + " exceeded: " + actorName.length());
        }
        this.checkpoint = checkpoint;
        this.loopCount = loopCount;
        this.ownedResources = Collections.unmodifiableList(ownedResources);
        if (ownedResources.size() > MAX_OWNED_RESOURCES) {
            throw new IllegalArgumentException("maximum number of resources exceeded: " + ownedResources.size());
        }
    }

    @Override
    public final String actorName() {
        return actorName;
    }

    @Override
    public final int checkpoint() {
        return checkpoint;
    }

    @Override
    public final int loopCount() {
        return loopCount;
    }

    @Override
    public final List<BlockingResource> ownedResources() {
        return this.ownedResources;
    }
}
