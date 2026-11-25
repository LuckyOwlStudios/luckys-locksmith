package net.luckyowlstudios.locksmith.network;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the mini game ended. The server checks it against the attempt it started. */
public record LockPickResultPayload(boolean success) implements CustomPacketPayload {
    public static final Type<LockPickResultPayload> TYPE = new Type<>(Locksmith.id("lock_pick_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockPickResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LockPickResultPayload::success,
            LockPickResultPayload::new);

    @Override
    public Type<LockPickResultPayload> type() { return TYPE; }
}
