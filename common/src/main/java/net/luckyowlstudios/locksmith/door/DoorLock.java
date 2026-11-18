package net.luckyowlstudios.locksmith.door;

import io.netty.buffer.ByteBuf;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The lock on one door or trapdoor.
 *
 * @param type  {@link LockType#LOCK} for a player lock opened by a key with {@code code}, or
 *              {@link LockType#GOLDEN} / {@link LockType#TRIAL} for the special locks
 * @param code  the key code of a player lock, empty for the special locks
 * @param owner the player who put the lock on; only known on the server
 */
public record DoorLock(LockType type, String code, @Nullable UUID owner) {

    /** Clients only need the type and code to predict whether a click opens the door. */
    public static final StreamCodec<ByteBuf, DoorLock> STREAM_CODEC = StreamCodec.composite(
            LockType.STREAM_CODEC, DoorLock::type,
            ByteBufCodecs.STRING_UTF8, DoorLock::code,
            (type, code) -> new DoorLock(type, code, null));

    public boolean isSpecial() {
        return type == LockType.GOLDEN || type == LockType.TRIAL;
    }
}
