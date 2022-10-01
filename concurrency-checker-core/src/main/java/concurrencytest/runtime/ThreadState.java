package concurrencytest.runtime;

import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.util.ByteBufferUtil;
import concurrencytest.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

public record ThreadState(String actorName, int checkpoint, int loopCount, List<LockMonitorAcquisition> ownedMonitors,
                          List<LockMonitorAcquisition> ownedLocks, Optional<LockMonitorAcquisition> waitingForMonitor,
                          Optional<LockMonitorAcquisition> waitingForLock,
                          Optional<String> waitingForThread, boolean finished) {

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
        var monitor = waitingForMonitor.map(LockMonitorAcquisition::lockOrMonitorId).filter(monId -> !state.ownedMonitors().containsKey(monId)).map(mon -> state.ownedMonitors().get(mon)).stream();
        var locks = waitingForLock.map(LockMonitorAcquisition::lockOrMonitorId).filter(monId -> !state.lockedLocks().containsKey(monId)).map(mon -> state.lockedLocks().get(mon)).stream();
        var conditionalWait = waitingForThread.map(actor -> state.actorNamesToThreadStates().get(actor)).stream();
        return Stream.concat(monitor, Stream.concat(locks, conditionalWait)).toList();
    }
//
//    public int writeTo(ByteBuffer buffer) {
//        int c = ByteBufferUtil.writeString(buffer, actorName);
//        c += ByteBufferUtil.writeVarInt(buffer, this.checkpoint);
//        c += ByteBufferUtil.writeVarInt(buffer, this.loopCount);
//        c += ByteBufferUtil.writeList(buffer, ownedMonitors, ByteBufferUtil::writeVarInt);
//        c += ByteBufferUtil.writeList(buffer, ownedLocks, ByteBufferUtil::writeVarInt);
//        int flags = waitingForMonitor.isPresent() ? WAITING_FOR_MONITOR_FLAG : 0;
//        flags += waitingForLock.isPresent() ? WAITING_FOR_LOCK_FLAG : 0;
//        flags += waitingForThread.isPresent() ? WAITING_FOR_THREAD : 0;
//        flags += finished ? FINISHED_FLAG : 0;
//        buffer.put((byte) flags);
//        c++;
//        c += waitingForMonitor.map(v -> ByteBufferUtil.writeVarInt(buffer, v)).orElse(0);
//        c += waitingForLock.map(v -> ByteBufferUtil.writeVarInt(buffer, v)).orElse(0);
//        c += waitingForThread.map(v -> ByteBufferUtil.writeString(buffer, v)).orElse(0);
//        return c;
//    }
//
//    public static ThreadState readFrom(ByteBuffer buffer) {
//        String actor = ByteBufferUtil.readString(buffer);
//        int checkpoint = ByteBufferUtil.readVarInt(buffer);
//        int loops = ByteBufferUtil.readVarInt(buffer);
//        List<Integer> ownedMonitors = ByteBufferUtil.readList(buffer, ByteBufferUtil::readVarInt);
//        List<Integer> ownedLocks = ByteBufferUtil.readList(buffer, ByteBufferUtil::readVarInt);
//        int flags = buffer.get();
//        Optional<Integer> waitingForMonitor = Optional.empty();
//        Optional<Integer> waitingForLock = Optional.empty();
//        Optional<String> waitingForThread = Optional.empty();
//        if ((flags & WAITING_FOR_MONITOR_FLAG) != 0) {
//            waitingForMonitor = Optional.of(ByteBufferUtil.readVarInt(buffer));
//        }
//        if ((flags & WAITING_FOR_LOCK_FLAG) != 0) {
//            waitingForLock = Optional.of(ByteBufferUtil.readVarInt(buffer));
//        }
//        if ((flags & WAITING_FOR_THREAD) != 0) {
//            waitingForThread = Optional.of(ByteBufferUtil.readString(buffer));
//        }
//        return new ThreadState(actor, checkpoint, loops, ownedMonitors, ownedLocks, waitingForMonitor, waitingForLock, waitingForThread, (flags & FINISHED_FLAG) != 0);
//    }

    public boolean canProceed(RuntimeState state) {
        return monitorIsFreeOrMine(state)
                && lockIsFreeOrMine(state)
                && waitingForThread.isEmpty();
    }

    public boolean lockIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(LockMonitorAcquisition::lockOrMonitorId).map(checkpointId1 -> state.ownedMonitors().get(checkpointId1) == null ||
                state.lockedLocks().get(checkpointId1).actorName().equals(actorName())).orElse(true);
    }

    public boolean monitorIsFreeOrMine(RuntimeState state) {
        return waitingForMonitor.map(LockMonitorAcquisition::lockOrMonitorId).map(checkpointId -> state.ownedMonitors().get(checkpointId) == null || state.ownedMonitors().get(checkpointId).actorName().equals(this.actorName())).orElse(true);
    }

    private void assertNotFinished() {
        if (finished()) {
            throw new IllegalStateException("tried operation on finished actor: %s".formatted(this.actorName));
        }
    }

    public ThreadState beforeLockAcquisition(int lockId, LockAcquireCheckpointDescription checkpointDescription) {
        assertNotFinished();
        if (this.waitingForLock().isPresent()) {
            throw new IllegalStateException("actor %s is already waiting for lock %s but its requesting another lock: %d".formatted(actorName, waitingForLock, lockId));
        }
        return new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, ownedLocks, waitingForMonitor, Optional.of(new LockMonitorAcquisition(lockId, checkpointDescription)), waitingForThread, false);
    }

    public ThreadState lockAcquired(int lockId) {
        assertNotFinished();
        return this.waitingForLock().filter(lma -> lma.lockOrMonitorId() == lockId).map(lma -> CollectionUtils.copyAndAdd(ownedLocks, lma))
                .map(l -> new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, l, waitingForMonitor, Optional.empty(), waitingForThread, false))
                .orElseThrow(() -> new IllegalStateException("actor %s was not waiting for lock %d but tried to acquire (was waiting for %s)".formatted(actorName, lockId, waitingForLock)));
    }

    public ThreadState lockReleased(int lockId) {
        assertNotFinished();
        List<LockMonitorAcquisition> newLocks = CollectionUtils.removeFirst(ownedLocks, en -> en.lockOrMonitorId() == lockId);
        if (newLocks.size() == ownedLocks.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, lockId, ownedLocks));
        }
        return new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, newLocks, waitingForMonitor, waitingForLock, waitingForThread, false);
    }

    public ThreadState beforeMonitorAcquire(int monitorId, MonitorCheckpointDescription description) {
        assertNotFinished();
        if (this.waitingForLock().isPresent()) {
            throw new IllegalStateException("actor %s is already waiting for monitor %s but its requesting another monitor: %d".formatted(actorName, waitingForMonitor, monitorId));
        }
        return new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, ownedLocks, Optional.of(new LockMonitorAcquisition(monitorId, description)), waitingForLock, waitingForThread, false);
    }

    public ThreadState monitorAcquired(int monitorId) {
        assertNotFinished();
        return this.waitingForMonitor.filter(lma -> lma.lockOrMonitorId() == monitorId)
                .map(lma -> CollectionUtils.copyAndAdd(ownedMonitors, lma))
                .map(lma -> new ThreadState(actorName, checkpoint, loopCount, lma, ownedLocks, waitingForMonitor, waitingForLock, waitingForThread, false))
                .orElseThrow(() -> new IllegalStateException("actor %s was not waiting for monitor %d but tried to acquire (was waiting for %s)".formatted(actorName, monitorId, waitingForMonitor)));
    }

    public ThreadState monitorReleased(int monitorId) {
        assertNotFinished();
        List<LockMonitorAcquisition> newMonitors = CollectionUtils.removeFirst(ownedMonitors, en -> en.lockOrMonitorId() == monitorId);
        if (newMonitors.size() == ownedMonitors.size()) {
            throw new IllegalStateException("actor %s tried to release a monitor (%d) that it did not held (%s)".formatted(actorName, monitorId, ownedMonitors));
        }
        return new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, newMonitors, waitingForMonitor, waitingForLock, waitingForThread, false);
    }

    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint, boolean lastCheckpoint) {
        return new ThreadState(actorName, newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount + 1 : 0, ownedMonitors, ownedLocks, waitingForMonitor, waitingForLock, waitingForThread, lastCheckpoint);
    }
}
