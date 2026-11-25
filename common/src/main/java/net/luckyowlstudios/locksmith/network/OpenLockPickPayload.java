package net.luckyowlstudios.locksmith.network;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: open the lock picking mini game. The owner's level sets how hard it is. */
public record OpenLockPickPayload(BlockPos pos, int ownerLevels) implements CustomPacketPayload {
    public static final Type<OpenLockPickPayload> TYPE = new Type<>(Locksmith.id("open_lock_pick"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLockPickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenLockPickPayload::pos,
            ByteBufCodecs.VAR_INT, OpenLockPickPayload::ownerLevels,
            OpenLockPickPayload::new);

    @Override
    public Type<OpenLockPickPayload> type() { return TYPE; }
}
