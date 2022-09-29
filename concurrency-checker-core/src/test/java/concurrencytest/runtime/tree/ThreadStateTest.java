package concurrencytest.runtime.tree;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.generator.java.lang.StringGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

@RunWith(JUnitQuickcheck.class)
public class ThreadStateTest {

    public static class ConstrainedStringGenerator extends StringGenerator {
        @Override
        public String generate(SourceOfRandomness random, GenerationStatus status) {
            int[] codePoints = new int[Math.min(255, status.size())];

            for (int i = 0; i < codePoints.length; ++i)
                codePoints[i] = nextCodePoint(random);

            return new String(codePoints, 0, codePoints.length);
        }
    }

    @Test
    public void emptyStateSerialization() {
        var ts = new ThreadState("");
        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        int amount = ts.writeTo(bb);
        System.out.printf("written %d%n", amount);
        bb.flip();
        Assert.assertEquals(amount, bb.remaining());
        Assert.assertTrue(amount < 7);
        ThreadState state = ThreadState.readFrom(bb);
        Assert.assertEquals(ts, state);
        Assert.assertEquals(0, bb.remaining());
    }

    @Property
    public void serializationPreservesField(@From(ConstrainedStringGenerator.class) String actorName, @InRange(minInt = 0) int checkpoint, @InRange(minInt = 0) int loopCount, @Size(max = 16) List<@InRange(minInt = 0) Integer> ownedMonitors,
                                            @Size(max = 16) List<@InRange(minInt = 0) Integer> ownedLocks, Optional<@InRange(minInt = 0) Integer> waitingForMonitor,
                                            Optional<@InRange(minInt = 0) Integer> waitingForLock, Optional<@From(ConstrainedStringGenerator.class) String> waitingForThread, boolean finished) {
        var ts = new ThreadState(actorName, checkpoint, loopCount, ownedMonitors, ownedLocks, waitingForMonitor, waitingForLock, waitingForThread, finished);
        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        int c = ts.writeTo(bb);
        bb.flip();
        Assert.assertEquals(c, bb.remaining());
        ThreadState state = ThreadState.readFrom(bb);
        Assert.assertEquals(ts, state);
        Assert.assertEquals(0, bb.remaining());
    }


}