package concurrencytest.runner.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class GenericStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericStatistics.class);

    private static final LongSupplier CPU_TIME_SUPPLIER;

    static {
        if(totalCPUUsageTimeNanosByOperatingSystemMX().getAsLong() >= 0) {
            CPU_TIME_SUPPLIER = totalCPUUsageTimeNanosByOperatingSystemMX();
        } else  {
            CPU_TIME_SUPPLIER = totalCPUUsageTimeNanosByThreadMXBean();
        }
    }

    public static long totalGCTimeNanos() {
        long collected = 0;
        List<GarbageCollectorMXBean> list = ManagementFactory.getGarbageCollectorMXBeans();
        for (var mx : list) {
            collected += TimeUnit.MILLISECONDS.toNanos(mx.getCollectionTime());
        }
        return collected;
    }

    private static LongSupplier totalCPUUsageTimeNanosByOperatingSystemMX() {
        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        try {
            Method method = bean.getClass().getMethod("getProcessCpuTime");
            return () -> {
                try {
                    return (Long) method.invoke(bean);
                } catch (Exception e) {
                    return -1;
                }
            };
        } catch (Exception e) {
            LOGGER.warn("Could not get cpu process time ", e);
        }
        return () -> -1L;
    }

    private static LongSupplier totalCPUUsageByProc() {
        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        try {
            Method method = bean.getClass().getMethod("getProcessCpuTime");
            return () -> {
                try {
                    return (Long) method.invoke(bean);
                } catch (Exception e) {
                    return -1;
                }
            };
        } catch (Exception e) {
            LOGGER.warn("Could not get cpu process time ", e);
        }
        return () -> -1L;
    }

    private static LongSupplier totalCPUUsageTimeNanosByThreadMXBean() {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        if (mxBean.isThreadCpuTimeEnabled() && mxBean.isThreadCpuTimeSupported()) {
            return () -> {
                long acc = 0;
                long[] allThreadIds = mxBean.getAllThreadIds();
                for (var id : allThreadIds) {
                    long c = mxBean.getThreadCpuTime(id);
                    if (c > 0) {
                        acc += c;
                    }
                }
                return acc;
            };
        }
        return () -> -1;
    }

    public static long totalCPUUsageTimeNanos() {
        return CPU_TIME_SUPPLIER.getAsLong();
    }

}
