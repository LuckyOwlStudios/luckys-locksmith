package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.platform.Services;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ModDataComponents {

    public static final Supplier<DataComponentType<LockType>> LOCK_TYPE = register("golden_lock",
            builder -> builder.persistent(LockType.CODEC).networkSynchronized(LockType.STREAM_CODEC));

    public static final Supplier<DataComponentType<UUID>> OWNER_UUID = register("owner_uuid",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    private static <T> Supplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Services.PLATFORM.register(BuiltInRegistries.DATA_COMPONENT_TYPE, name,
                () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void init() {
    }
}
