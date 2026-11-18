package net.luckyowlstudios.locksmith.network;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** A complete visual snapshot of one chunk. Never sends key codes or owners. */
public record DoorLocksPayload(ResourceLocation dimension, long chunk, Map<BlockPos, LockType> locks)
        implements CustomPacketPayload {
    public static final Type<DoorLocksPayload> TYPE = new Type<>(Locksmith.id("door_locks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoorLocksPayload> STREAM_CODEC =
            StreamCodec.of(DoorLocksPayload::write, DoorLocksPayload::read);

    public DoorLocksPayload {
        locks = Map.copyOf(locks);
    }

    private static void write(RegistryFriendlyByteBuf buffer, DoorLocksPayload payload) {
        buffer.writeResourceLocation(payload.dimension);
        buffer.writeLong(payload.chunk);
        buffer.writeVarInt(payload.locks.size());
        payload.locks.forEach((pos, type) -> {
            buffer.writeBlockPos(pos);
            buffer.writeEnum(type);
        });
    }

    private static DoorLocksPayload read(RegistryFriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        long chunk = buffer.readLong();
        int size = buffer.readVarInt();
        if (size < 0 || size > 65536) throw new IllegalArgumentException("Invalid door lock snapshot size");
        Map<BlockPos, LockType> locks = new HashMap<>();
        for (int i = 0; i < size; i++) locks.put(buffer.readBlockPos(), buffer.readEnum(LockType.class));
        return new DoorLocksPayload(dimension, chunk, locks);
    }

    @Override
    public Type<DoorLocksPayload> type() { return TYPE; }
}
