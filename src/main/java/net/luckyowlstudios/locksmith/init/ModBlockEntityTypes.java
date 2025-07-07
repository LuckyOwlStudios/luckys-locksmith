package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Locksmith.MOD_ID);

    public static final Supplier<BlockEntityType<IronChestBlockEntity>> IRON_CHEST_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("iron_chest", () -> BlockEntityType.Builder.of(
                    IronChestBlockEntity::new,
                    ModBlocks.IRON_CHEST.get()
                    )
                    .build(null));

    public static final Supplier<BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("golden_chest", () -> BlockEntityType.Builder.of(
                            GoldenChestBlockEntity::new,
                            ModBlocks.GOLDEN_CHEST.get()
                    )
                    .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
