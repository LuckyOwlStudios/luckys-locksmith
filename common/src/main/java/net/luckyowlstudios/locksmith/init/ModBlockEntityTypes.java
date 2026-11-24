package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.jeweled.JeweledChestBlockEntity;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestBlockEntity;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModBlockEntityTypes {

    public static final Supplier<BlockEntityType<IronChestBlockEntity>> IRON_CHEST_BLOCK_ENTITY =
            register("iron_chest", () -> Services.PLATFORM.createBlockEntityType(IronChestBlockEntity::new, ModBlocks.IRON_CHEST.get()));

    public static final Supplier<BlockEntityType<GoldenChestBlockEntity>> GOLDEN_CHEST_BLOCK_ENTITY =
            register("golden_chest", () -> Services.PLATFORM.createBlockEntityType(GoldenChestBlockEntity::new, ModBlocks.GOLDEN_CHEST.get()));

    public static final Supplier<BlockEntityType<JeweledChestBlockEntity>> JEWELED_CHEST_BLOCK_ENTITY =
            register("jeweled_chest", () -> Services.PLATFORM.createBlockEntityType(JeweledChestBlockEntity::new, ModBlocks.JEWELED_CHEST.get()));

    public static final Supplier<BlockEntityType<IronTrappedChestBlockEntity>> IRON_TRAPPED_CHEST_BLOCK_ENTITY =
            register("iron_trapped_chest", () -> Services.PLATFORM.createBlockEntityType(IronTrappedChestBlockEntity::new, ModBlocks.IRON_TRAPPED_CHEST.get()));

    public static final Supplier<BlockEntityType<GoldenTrappedChestBlockEntity>> GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY =
            register("golden_trapped_chest", () -> Services.PLATFORM.createBlockEntityType(GoldenTrappedChestBlockEntity::new, ModBlocks.GOLDEN_TRAPPED_CHEST.get()));

    private static <T extends BlockEntityType<?>> Supplier<T> register(String name, Supplier<T> type) {
        return Services.PLATFORM.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, type);
    }

    public static void init() {
    }
}
