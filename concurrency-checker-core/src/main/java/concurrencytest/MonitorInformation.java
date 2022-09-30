package concurrencytest;


import concurrencytest.util.InstrospectionHelper;

import java.util.Objects;

public class MonitorInformation {

    private final int identity;
    private final Class<?> type;
    private final String details;

    public MonitorInformation(int identity, Class<?> type) {
        this(identity, type, InstrospectionHelper.findRelevantStackInfo());
    }

    public MonitorInformation(int identity, Class<?> type, String acquisitionDetails) {
        this.identity = identity;
        this.type = type;
        this.details = acquisitionDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitorInformation that = (MonitorInformation) o;
        return identity == that.identity && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, type);
    }

    @Override
    public String toString() {
        return "LockOrMonitorInformation{" +
                "identity=" + identity +
                ", type=" + type +
                ", details='" + details + '\'' +
                '}';
    }
}
