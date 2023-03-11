package concurrencytest.runtime.tree;

import concurrencytest.util.ByteBufferUtil;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ActorInformation(String actorName, int checkpointId, int loopCount, List<ResourceInformation> resourcesOwned,
                               Optional<BlockingCause> blockedBy, boolean finished) {

    public ActorInformation(String actorName, int initialCheckpointId) {
        this(actorName, initialCheckpointId, 0, Collections.emptyList(), Optional.empty(), false);
    }

    public ActorInformation {
        Objects.requireNonNull(resourcesOwned, "resources cannot be null");
        Objects.requireNonNull(blockedBy, "blockedBy cannot be null");
        if (actorName == null || actorName.isEmpty()) {
            throw new IllegalArgumentException("actor cannot be null or empty");
        }
    }

    public boolean isBlocked() {
        return blockedBy.filter(cause -> cause.blocksActor(actorName)).isPresent();
    }

    public static final int BLOCKED_FLAG = 1;
    public static final int WAITING_FOR_LOCK_FLAG = 2;
    public static final int FINISHED_FLAG = 2;

    public int writeToByteBuffer(ByteBuffer buffer) {
        int c = ByteBufferUtil.writeString(buffer, actorName);
        c += ByteBufferUtil.writeVarInt(buffer, checkpointId);
        c += ByteBufferUtil.writeVarInt(buffer, loopCount);
        c += ByteBufferUtil.writeCollection(buffer, resourcesOwned, (a, b) -> b.writeToByteBuffer(a));
        byte flags = (byte) ((blockedBy.isPresent() ? BLOCKED_FLAG : 0) + (finished ? FINISHED_FLAG : 0));
        buffer.put(flags);
        c++;
        c += blockedBy().map(cause -> cause.writeToByteBuffer(buffer)).orElse(0);
        return c;
    }

    public static ActorInformation readFromBuffer(ByteBuffer byteBuffer) {
        byteBuffer.mark(); // TODO why does this method mark / reset the buffer?
        String actorName = ByteBufferUtil.readString(byteBuffer);
        int checkpoint = ByteBufferUtil.readVarInt(byteBuffer);
        int loop = ByteBufferUtil.readVarInt(byteBuffer);
        List<ResourceInformation> resources = ByteBufferUtil.readList(byteBuffer, ResourceInformation::readFromBuffer);
        int flags = byteBuffer.get() & 0xff;
        Optional<BlockingCause> blockedBy = (flags & BLOCKED_FLAG) != 0 ? Optional.of(BlockingCause.readFromBuffer(byteBuffer)) : Optional.empty();
        byteBuffer.reset();
        return new ActorInformation(actorName, checkpoint, loop, resources, blockedBy, (flags & FINISHED_FLAG) != 0);
    }

}
