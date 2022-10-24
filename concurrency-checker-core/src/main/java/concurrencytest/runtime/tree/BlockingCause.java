package concurrencytest.runtime.tree;

import concurrencytest.runtime.lock.BlockCauseType;
import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.Optional;

public record BlockingCause(BlockCauseType type, Optional<String> resourceHolder) {

    public int writeToByteBuffer(ByteBuffer buffer) {
        int c = 0;
        buffer.put((byte) type.ordinal());
        c += ByteBufferUtil.writeString(buffer, resourceHolder.orElse(""));
        return c;
    }

    public static BlockingCause readFromBuffer(ByteBuffer buffer) {
        int type = buffer.get() & 0xff;
        String s = ByteBufferUtil.readString(buffer);
        return new BlockingCause(BlockCauseType.values()[type], s.isEmpty() ? Optional.empty() : Optional.of(s));
    }

    public boolean blocksActor(String actorName) {
        return switch (type) {
            case LOCK, MONITOR -> resourceHolder.isPresent() && !resourceHolder.get().equals(actorName);
            default -> true;
        };
    }
}
