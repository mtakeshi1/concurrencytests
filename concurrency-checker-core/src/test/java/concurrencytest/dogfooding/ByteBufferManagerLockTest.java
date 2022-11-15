package concurrencytest.dogfooding;


import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import concurrencytest.annotations.v2.ConfigurationSource;
import concurrencytest.asm.ArrayElementMatcher;
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
import org.junit.Ignore;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Ignore
@RunWith(ActorSchedulerRunner.class)
public class ByteBufferManagerLockTest {

    private final List<Pair> workingPairs = new CopyOnWriteArrayList<>();

    private ByteBufferManager byteBufferManager;

    @Before
    public void setup() {
        byteBufferManager = new ByteBufferManagerImpl();
    }

    @Actor
    public void actorPre() throws IOException {
        RecordEntry entry = allocateAndWork(128);
        Assert.assertNotNull(entry);
//        Thread.sleep(10);
        RecordEntry entry2 = allocateAndWork(256);
        Assert.assertNotNull(entry2);
    }

    @Actor
    public void actorPost() throws IOException {
        RecordEntry recordEntry = allocateAndWork(2048);
        Assert.assertNotNull(recordEntry);
    }

    private RecordEntry allocateAndWork(int size) throws IOException {
        RecordEntry region = byteBufferManager.allocateNewSlice(256);
        Pair pair = new Pair(region.absoluteOffset(), region.recordSize());
        region.writeToRecordNoReturn(b -> innerThing(pair));
        return region;
    }

    private void innerThing(Pair pair) {
        workingPairs.add(pair);
        CheckpointRuntimeAccessor.manualCheckpoint("with lock maybe");
        workingPairs.remove(pair);
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
            public int parallelExecutions() {
                return 12;
            }

            @Override
            public CheckpointConfiguration checkpointConfiguration() {
                return new CheckpointConfiguration() {
                    @Override
                    public boolean includeStandardMethods() {
                        return false;
                    }

                    @Override
                    public Collection<ArrayElementMatcher> arrayCheckpoints() {
                        return Collections.emptyList();
                    }
                };
            }
        };
    }

}
