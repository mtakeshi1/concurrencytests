package concurrencytest.runtime.tree;

import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

public record ThreadState(String actorName, int checkpoint, int loopCount, List<Integer> ownedMonitors, List<Integer> ownedLocks,
                          Optional<Integer> waitingForMonitor, Optional<Integer> waitingForLock, Optional<String> waitingForThread,
                          boolean finished) {

    public static final int MAX_ACTOR_NAME_LENGTH = 255;
    public static final int MAX_OWNED_MONITOR_LIST = 16;

    public ThreadState(String actorName) {
        this(actorName, 0, 0, Collections.emptyList(), Collections.emptyList(), Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    public ThreadState {
        if (Objects.requireNonNull(actorName).length() > 255) {
            throw new IllegalArgumentException("name cannot have length > %d".formatted(MAX_ACTOR_NAME_LENGTH));
        }
        if (Objects.requireNonNull(ownedMonitors).size() > MAX_OWNED_MONITOR_LIST) {
            throw new IllegalArgumentException("list of owned monitors cannot be > %d".formatted(MAX_OWNED_MONITOR_LIST));
        }
        if (Objects.requireNonNull(ownedLocks).size() > MAX_OWNED_MONITOR_LIST) {
            throw new IllegalArgumentException("list of owned locks cannot be > %d".formatted(MAX_OWNED_MONITOR_LIST));
        }
        Objects.requireNonNull(waitingForLock, "waiting for locks cannot be null");
        Objects.requireNonNull(waitingForMonitor, "waiting for monitor cannot be null");
        Objects.requireNonNull(waitingForThread, "waiting for threads cannot be null");
    }

    private static final int WAITING_FOR_MONITOR_FLAG = 1;
    private static final int WAITING_FOR_LOCK_FLAG = 2;
    private static final int WAITING_FOR_THREAD = 4;

    private static final int FINISHED_FLAG = 8;

    public boolean runnable() {
        return !finished();
    }

    public Collection<ThreadState> dependencies(RuntimeState state) {
        Stream<ManagedThread> monitor = waitingForMonitor.filter(monId -> !state.ownedMonitors().containsKey(monId)).map(mon -> state.ownedMonitors().get(mon)).stream();
        Stream<ManagedThread> locks = waitingForLock.filter(monId -> !state.lockedLocks().containsKey(monId)).map(mon -> state.lockedLocks().get(mon)).stream();
        Stream<ManagedThread> conditionalWait = waitingForThread.map(actor -> state.actorNamesToThreads().get(actor)).stream();
        return Stream.concat(monitor, Stream.concat(locks, conditionalWait)).toList();
    }

    public int writeTo(ByteBuffer buffer) {
        int c = ByteBufferUtil.writeString(buffer, actorName);
        c += ByteBufferUtil.writeVarInt(buffer, this.checkpoint);
        c += ByteBufferUtil.writeVarInt(buffer, this.loopCount);
        c += ByteBufferUtil.writeList(buffer, ownedMonitors, ByteBufferUtil::writeVarInt);
        c += ByteBufferUtil.writeList(buffer, ownedLocks, ByteBufferUtil::writeVarInt);
        int flags = waitingForMonitor.isPresent() ? WAITING_FOR_MONITOR_FLAG : 0;
        flags += waitingForLock.isPresent() ? WAITING_FOR_LOCK_FLAG : 0;
        flags += waitingForThread.isPresent() ? WAITING_FOR_THREAD : 0;
        flags += finished ? FINISHED_FLAG : 0;
        buffer.put((byte) flags);
        c++;
        c += waitingForMonitor.map(v -> ByteBufferUtil.writeVarInt(buffer, v)).orElse(0);
        c += waitingForLock.map(v -> ByteBufferUtil.writeVarInt(buffer, v)).orElse(0);
        c += waitingForThread.map(v -> ByteBufferUtil.writeString(buffer, v)).orElse(0);
        return c;
    }

    public static ThreadState readFrom(ByteBuffer buffer) {
        String actor = ByteBufferUtil.readString(buffer);
        int checkpoint = ByteBufferUtil.readVarInt(buffer);
        int loops = ByteBufferUtil.readVarInt(buffer);
        List<Integer> ownedMonitors = ByteBufferUtil.readList(buffer, ByteBufferUtil::readVarInt);
        List<Integer> ownedLocks = ByteBufferUtil.readList(buffer, ByteBufferUtil::readVarInt);
        int flags = buffer.get();
        Optional<Integer> waitingForMonitor = Optional.empty();
        Optional<Integer> waitingForLock = Optional.empty();
        Optional<String> waitingForThread = Optional.empty();
        if ((flags & WAITING_FOR_MONITOR_FLAG) != 0) {
            waitingForMonitor = Optional.of(ByteBufferUtil.readVarInt(buffer));
        }
        if ((flags & WAITING_FOR_LOCK_FLAG) != 0) {
            waitingForLock = Optional.of(ByteBufferUtil.readVarInt(buffer));
        }
        if ((flags & WAITING_FOR_THREAD) != 0) {
            waitingForThread = Optional.of(ByteBufferUtil.readString(buffer));
        }
        return new ThreadState(actor, checkpoint, loops, ownedMonitors, ownedLocks, waitingForMonitor, waitingForLock, waitingForThread, (flags & FINISHED_FLAG) != 0);
    }

    public boolean canProceed(RuntimeState state) {
        return monitorIsFreeOrMine(state)
                && lockIsFreeOrMine(state)
                && waitingForThread.isEmpty();
    }

    public boolean lockIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId1 -> state.ownedMonitors().get(checkpointId1) == null ||
                state.lockedLocks().get(checkpointId1).getActorName().equals(actorName())).orElse(true);
    }

    public boolean monitorIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(checkpointId -> state.ownedMonitors().get(checkpointId) == null || state.ownedMonitors().get(checkpointId).getActorName().equals(this.actorName())).orElse(true);
    }

}
