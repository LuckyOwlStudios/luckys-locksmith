package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlock;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Locksmith.MOD_ID);

    public static final DeferredBlock<Block> IRON_CHEST = registerBlock("iron_chest",
            () -> new IronChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> GOLDEN_CHEST = registerBlock("golden_chest",
            () -> new GoldenChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)));

    public static final DeferredBlock<Block> IRON_TRAPPED_CHEST = registerBlock("iron_trapped_chest",
            () -> new IronTrappedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    public static final DeferredBlock<Block> GOLDEN_TRAPPED_CHEST = registerBlock("golden_trapped_chest",
            () -> new GoldenTrappedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
