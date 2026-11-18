package net.luckyowlstudios.locksmith.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum LockType implements StringRepresentable {
    NONE(0,"none"),
    LOCK(1,"lock"),
    GOLDEN(2,"golden"),
    TRIAL(3,"trial");

    public static final Codec<LockType> CODEC = StringRepresentable.fromValues(LockType::values);
    public static final IntFunction<LockType> BY_ID = ByIdMap.continuous((p_335877_) -> p_335877_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, LockType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, (p_335484_) -> p_335484_.id);
    private final int id;
    private final String name;

    LockType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getSerializedName() {
        return this.name;
    }
}
