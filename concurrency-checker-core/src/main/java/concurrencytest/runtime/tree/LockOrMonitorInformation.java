package concurrencytest.runtime.tree;

import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.Optional;

public record LockOrMonitorInformation(String monitorType, Optional<String> ownerActorName, String source, int lineNumber) {
    public static final int OWNER_PRESENT_FLAG = 1;

    public boolean isBlocked(String selfName) {
        return ownerActorName().isEmpty() || ownerActorName.get().equals(selfName);
    }

    public int writeToByteBuffer(ByteBuffer buffer) {
        int c = ByteBufferUtil.writeString(buffer, monitorType);

        if (ownerActorName.isPresent()) {
            buffer.put((byte) OWNER_PRESENT_FLAG);
            c += ByteBufferUtil.writeString(buffer, ownerActorName.get());
        } else {
            buffer.put((byte) 0);
        }
        c++;
        c += ByteBufferUtil.writeString(buffer, source);
        if (lineNumber < 0) {
            c += ByteBufferUtil.writeVarInt(buffer, 0);
        } else {
            c += ByteBufferUtil.writeVarInt(buffer, lineNumber);
        }
        return c;
    }

    public static LockOrMonitorInformation readFromBuffer(ByteBuffer buffer) {
        String monitorType = ByteBufferUtil.readString(buffer);
        Optional<String> owner = Optional.empty();
        if ((buffer.get() & OWNER_PRESENT_FLAG) != 0) {
            owner = Optional.of(ByteBufferUtil.readString(buffer));
        }
        String source = ByteBufferUtil.readString(buffer);
        int line = ByteBufferUtil.readVarInt(buffer);
        return new LockOrMonitorInformation(monitorType, owner, source, line);
    }

}
