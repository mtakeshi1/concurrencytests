package concurrencytest.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

public class ByteBufferUtil {

    public static long readVarLong(ByteBuffer buffer) {
        long c = 0;
        for (int i = 0; i < 9; i++) {
            long n = buffer.get() & 0xff;
            c |= (n & 0x7F) << (7 * i);
            boolean hasMore = (n & ~0x7F) != 0;
            if (!hasMore) {
                return c;
            }
        }
        return c;
    }


    public static int writeVarLong(ByteBuffer buffer, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("cannot write negative number");
        }
        int i;
        for (i = 0; i < 9; ) {
            boolean hasMore = (value & ~0x7F) != 0;
            byte toWrite = (byte) (value & 0x7F);
            if (hasMore) {
                toWrite |= 0x80;
            }
            buffer.put(toWrite);
            i++;
            if (!hasMore) {
                break;
            }
            value >>>= 7;
        }
        return i;
    }

    public static int readVarInt(ByteBuffer buffer) {
        int c = 0;
        for (int i = 0; i < 5; i++) {
            int n = buffer.get() & 0xff;
            c |= (n & 0x7F) << (7 * i);
            boolean hasMore = (n & ~0x7F) != 0;
            if (!hasMore) {
                return c;
            }
//            c <<= 7;
        }
        return c;
    }

    public static int writeVarInt(ByteBuffer buffer, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("cannot write negative number");
        }
        int i;
        for (i = 0; i < 5; ) {
            boolean hasMore = (value & ~0x7F) != 0;
            byte toWrite = (byte) (value & 0x7F);
            if (hasMore) {
                toWrite |= 0x80;
            }
            buffer.put(toWrite);
            i++;
            if (!hasMore) {
                break;
            }
            value >>>= 7;
        }
        return i;
    }

    public static int writeString(ByteBuffer buffer, String value) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        int c = writeVarInt(buffer, data.length);
        buffer.put(data);
        return c + data.length;
    }

    public static String readString(ByteBuffer buffer) {
        int len = readVarInt(buffer);
        byte[] data = new byte[len];
        buffer.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static <E> int writeCollection(ByteBuffer buffer, Collection<? extends E> list, ToIntBiFunction<ByteBuffer, E> function) {
        int c = writeVarInt(buffer, list.size());
        for (var e : list) {
            c += function.applyAsInt(buffer, e);
        }
        return c;
    }

    public static <E> List<E> readList(ByteBuffer buffer, Function<ByteBuffer, E> mapper) {
        int els = readVarInt(buffer);
        var list = new ArrayList<E>(els);
        for (int i = 0; i < els; i++) {
            list.add(mapper.apply(buffer));
        }
        return list;
    }

    public static long readLong6Bytes(ByteBuffer buffer) {
        return readLong6Bytes(buffer,0);
    }

    public static void writeLong6Bytes(ByteBuffer buffer, long offset) {
        if (offset < 0) {
            throw new RuntimeException("number cannot be negative: %d".formatted(offset));
        }
        for (int i = 0; i < 6; i++) {
            buffer.put((byte) (offset & 0xff));
            offset >>= 8;
        }
    }

    public static long readLong6Bytes(ByteBuffer buffer, int offset) {
        long c = 0;
        for (int i = 0; i < 6; i++) {
            c += (buffer.get(offset + i) & 0xffL) << 8 * i;
        }
        return c;
    }

    public static void writeInt2Bytes(ByteBuffer buffer, int val) {
        throw new RuntimeException();
    }

    public static int readInt2Bytes(ByteBuffer slice) {
        throw new RuntimeException();
    }
}
