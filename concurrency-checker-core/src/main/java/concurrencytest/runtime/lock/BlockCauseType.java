package concurrencytest.runtime.lock;

public enum BlockCauseType {
    MONITOR, LOCK, THREAD_JOIN, FUTURE_GET, LOCK_SUPPORT_PARK, OBJECT_WAIT, CONDITION_AWAIT
}
