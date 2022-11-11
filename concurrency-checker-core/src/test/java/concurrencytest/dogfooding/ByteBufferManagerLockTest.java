package concurrencytest.dogfooding;


import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.MultipleActors;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.config.BasicConfiguration;
import concurrencytest.config.CheckpointConfiguration;
import concurrencytest.config.Configuration;
import concurrencytest.config.ExecutionMode;
import concurrencytest.runner.ActorSchedulerRunner;
import concurrencytest.runtime.CheckpointRuntimeAccessor;
import concurrencytest.runtime.tree.ByteBufferManagerImpl;
import concurrencytest.runtime.tree.InVmRegionLock;
import concurrencytest.runtime.tree.offheap.AbstractByteBufferManager;
import concurrencytest.runtime.tree.offheap.ByteBufferManager;
import concurrencytest.runtime.tree.offheap.ByteBufferManager.RecordEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RunWith(ActorSchedulerRunner.class)
public class ByteBufferManagerLockTest {

    private List<Pair> workingPairs = new CopyOnWriteArrayList<>();


    private ByteBufferManager byteBufferManager;

    @Before
    public void setup() {
        byteBufferManager = new ByteBufferManagerImpl();
    }

    @MultipleActors(numberOfActors = 2)
    public void actor() throws IOException {
        RecordEntry region = byteBufferManager.allocateNewSlice(1024);
        region.writeToRecordNoReturn(b -> {
            Pair pair = new Pair(region.absoluteOffset(), region.recordSize());
            workingPairs.add(pair);
            CheckpointRuntimeAccessor.manualCheckpoint("with lock maybe");
            workingPairs.remove(pair);
        });
    }


    @Invariant
    public void atMostOneActor() {
        for (int i = 0; i < workingPairs.size(); i++) {
            Pair p0 = workingPairs.get(i);
            for (int j = i + 1; j < workingPairs.size(); j++) {
                Pair p1 = workingPairs.get(j);
                Assert.assertFalse("overlapping of pairs: " + p0 + " and " + p1, p0.overlaps(p1));
            }
        }
    }

    @ConfigurationSource
    public static Configuration config() {
        return new BasicConfiguration(ByteBufferManagerLockTest.class) {
            @Override
            public Collection<Class<?>> classesToInstrument() {
                return List.of(ByteBufferManagerImpl.class, AbstractByteBufferManager.class, InVmRegionLock.class);
            }

            @Override
            public ExecutionMode executionMode() {
                return ExecutionMode.CLASSLOADER_ISOLATION;
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public boolean includeStandardMethods() {
                        return true;
                    }
                };
            }
        };
    }

}
