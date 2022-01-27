package concurrencytest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockMonitorObserver {

    private final Map<MonitorInformation, Queue<ManagedThread>> ownedMonitor = new ConcurrentHashMap<>();

    private final Map<ManagedThread, MonitorInformation> waitingForMonitor = new ConcurrentHashMap<>();

    private final String type;

    public LockMonitorObserver(String type) {
        this.type = type;
    }

    public synchronized void waitingForMonitor(ManagedThread thread, Object object, String description) {
        if (object == null) {
            throw new NullPointerException("cannot wait on null object");
        }
        waitingForMonitor.put(thread, new MonitorInformation(System.identityHashCode(object), object.getClass(), description));
    }


    public synchronized void signalMonitorAcquired(ManagedThread thread) {
        MonitorInformation info = waitingForMonitor.get(thread);
        if (info == null) {
            throw new RuntimeException(thread + " Tried to acquire " + this.type + " but was not waiting for " + type);
        }
        Queue<ManagedThread> queue = ownedMonitor.computeIfAbsent(info, m -> new LinkedList<>());
        if (queue.peek() != null && queue.peek() != thread) {
            throw new RuntimeException("Tried to acquire " + type + " for " + info + " but it was already owned by: " + queue.peek());
        }
        queue.add(thread);
        waitingForMonitor.remove(thread);
    }

    public synchronized void monitorReleased(ManagedThread thread, Object monitor) {
        MonitorInformation info = new MonitorInformation(System.identityHashCode(monitor), monitor.getClass(), "");
        Queue<ManagedThread> queue = ownedMonitor.computeIfAbsent(info, m -> new LinkedList<>());
        if (queue.peek() != thread) {
            throw new RuntimeException("Tried to acquire " + type + " for " + info + " but it was already owned by: " + queue.peek());
        }
        queue.poll();
        if (queue.isEmpty()) {
            ownedMonitor.remove(info, queue);
        }
    }

    public synchronized String blockedInformation(ManagedThread thread) {
        MonitorInformation information = waitingForMonitor.get(thread);
        if (information == null) {
            return null;
        }
        Queue<ManagedThread> queue = ownedMonitor.get(information);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        ManagedThread peek = queue.peek();
        if (peek != thread) {
            return "BLOCKED waiting for " + type + " on " + information + " which is owned by: " + peek;
        }
        return null;
    }

    public synchronized boolean isBlocked(ManagedThread thread) {
        MonitorInformation information = waitingForMonitor.get(thread);
        if (information == null) {
            return false;
        }
        Queue<ManagedThread> queue = ownedMonitor.get(information);
        if (queue == null || queue.isEmpty()) {
            return false;
        }
        return queue.peek() != thread;
    }

    public synchronized boolean isWaitingFor(ManagedThread managedThread) {
        return waitingForMonitor.get(managedThread) != null;
    }

    public synchronized void signalMonitorAcquiredIfNecessary(ManagedThread managedThread) {
        MonitorInformation info = waitingForMonitor.get(managedThread);
        if (info != null) {
            signalMonitorAcquired(managedThread);
        }

    }

    public synchronized Collection<ManagedThread> findDependenciesFor(ManagedThread thread, Set<ManagedThread> exclusion) {
        return null;
    }

    public synchronized void checkThreadFinished(ManagedThread managedThread) {
        MonitorInformation information = waitingForMonitor.get(managedThread);
        if (information != null) {
            throw new RuntimeException("thread " + managedThread + " is waiting for " + type + ": " + information + " but was finished");
        }
        for (var entry : this.ownedMonitor.entrySet()) {
            if (entry.getValue() != null && entry.getValue().peek() == managedThread) {
                throw new RuntimeException("thread " + managedThread + " was holding " + type + " but was finished. Details: " + entry.getKey());
            }
        }
    }
}
