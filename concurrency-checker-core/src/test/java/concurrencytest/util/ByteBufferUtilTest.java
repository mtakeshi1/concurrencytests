package concurrencytest.util;


import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Random;


@RunWith(JUnitQuickcheck.class)
public class ByteBufferUtilTest {

    @Test
    public void testWriteVarIntSingleChunk() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        for (int i = 0; i < 0x80; i++) {
            bb.clear();
            int written = ByteBufferUtil.writeVarInt(bb, i);
            Assert.assertEquals(1, written);
            bb.flip();
            int r = ByteBufferUtil.readVarInt(bb);
            Assert.assertEquals(i, r);
        }
    }

    @Test
    public void testWriteVarInt23Chunks() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        for (int i = 0x80; i < (1 << 14); i++) {
            bb.clear();
            int written = ByteBufferUtil.writeVarInt(bb, i);
            Assert.assertEquals("for int " + i, 2, written);
            bb.flip();
            int r = ByteBufferUtil.readVarInt(bb);
            Assert.assertEquals(i, r);
        }
        for (int i = (1 << 14); i < (1 << 21); i++) {
            bb.clear();
            int written = ByteBufferUtil.writeVarInt(bb, i);
            Assert.assertEquals("for int " + i, 3, written);
            bb.flip();
            int r = ByteBufferUtil.readVarInt(bb);
            Assert.assertEquals(i, r);
        }
    }

    @Test
    public void testRandomSample() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            int v = rnd.nextInt(Integer.MAX_VALUE);
            bb.clear();
            int written = ByteBufferUtil.writeVarInt(bb, v);
            Assert.assertTrue(written <= 5);
            bb.flip();
            int r = ByteBufferUtil.readVarInt(bb);
            Assert.assertEquals("error for value: " + v, v, r);
        }
    }

    @Property
    public void encodeDecodeInt(@InRange(min = "0") int val) {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.clear();
        int written = ByteBufferUtil.writeVarInt(bb, val);
        Assert.assertTrue(written <= 5);
        bb.flip();
        int r = ByteBufferUtil.readVarInt(bb);
        Assert.assertEquals("error for value: " + val, val, r);
    }

    @Property
    public void encodeDecodeLong(@InRange(min = "0") long val) {
        ByteBuffer bb = ByteBuffer.allocate(10);
        bb.clear();
        int written = ByteBufferUtil.writeVarLong(bb, val);
        Assert.assertTrue(written <= 9);
        bb.flip();
        long r = ByteBufferUtil.readVarLong(bb);
        Assert.assertEquals("error for value: " + val, val, r);
    }

}