package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponents {

    public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_REGISTRY_KEY =
            Registries.DATA_COMPONENT_TYPE;

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(DATA_COMPONENT_REGISTRY_KEY, Locksmith.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LockType>> LOCK_TYPE = register("golden_lock",
            builder -> builder.persistent(LockType.CODEC).networkSynchronized(LockType.STREAM_CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
                                                                                           UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
