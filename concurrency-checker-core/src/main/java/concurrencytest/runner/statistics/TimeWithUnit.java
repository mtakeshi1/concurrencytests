package concurrencytest.runner.statistics;

import java.util.concurrent.TimeUnit;

public record TimeWithUnit(double time, TimeUnit unit) {
    public static String unitName(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "us";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            default -> "-";
        };
    }

    public String format() {
        double nom = time;
        int unitIndex = TimeUnit.NANOSECONDS.ordinal();
        while (nom > 1000 && unitIndex < TimeUnit.SECONDS.ordinal()) {
            nom /= 1000.0;
            unitIndex++;
        }
        return "%.2f%s".formatted(nom, unitName(TimeUnit.values()[unitIndex]));
    }

}
