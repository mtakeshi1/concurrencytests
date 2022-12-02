package concurrencytest.basic.asm.testClasses;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointDurationConfiguration;
import concurrencytest.config.Configuration;

import java.time.Duration;

public class MonitorWait {

    private volatile boolean canResume;

    @Actor
    public void actor() throws InterruptedException {
        while (!canResume) {
            synchronized (this) {
                this.wait();
            }
        }
    }


    @Actor
    public void actorNotify() {
        synchronized (this) {
            canResume = true;
            this.notifyAll();
        }
    }

    @ConfigurationSource
    public static Configuration configuration() {
        return new BasicConfiguration(MonitorWait.class) {
            @Override
            public CheckpointDurationConfiguration durationConfiguration() {
                return new CheckpointDurationConfiguration(Duration.ofMinutes(1), Duration.ofMinutes(1), Duration.ofMinutes(1));
            }
        };
    }

}
