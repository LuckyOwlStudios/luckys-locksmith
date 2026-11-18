package net.luckyowlstudios.locksmith.door;

import io.netty.buffer.ByteBuf;
import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Server to client: every door lock in one chunk, replacing what the client knew about that chunk.
 * Sent when a chunk is sent to a player and whenever a lock in it changes.
 */
public record DoorLockSyncPayload(long chunkPos, List<Entry> entries) implements CustomPacketPayload {

    public static final Type<DoorLockSyncPayload> TYPE = new Type<>(Locksmith.id("door_locks"));

    public static final StreamCodec<ByteBuf, DoorLockSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DoorLockSyncPayload::chunkPos,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), DoorLockSyncPayload::entries,
            DoorLockSyncPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(BlockPos pos, DoorLock lock) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos,
                DoorLock.STREAM_CODEC, Entry::lock,
                Entry::new);
    }
}
