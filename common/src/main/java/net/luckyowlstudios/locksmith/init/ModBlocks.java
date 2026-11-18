package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestBlock;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlock;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestBlock;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ModBlocks {

    private static final List<Supplier<Block>> ALL = new ArrayList<>();
    /** Every block this mod registers, in registration order. */
    public static final List<Supplier<Block>> BLOCKS = Collections.unmodifiableList(ALL);

    public static final Supplier<Block> IRON_CHEST = registerBlock("iron_chest",
            () -> new IronChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.WOOD)));

    public static final Supplier<Block> GOLDEN_CHEST = registerBlock("golden_chest",
            () -> new GoldenChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).sound(SoundType.WOOD)));

    public static final Supplier<Block> IRON_TRAPPED_CHEST = registerBlock("iron_trapped_chest",
            () -> new IronTrappedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.WOOD)));

    public static final Supplier<Block> GOLDEN_TRAPPED_CHEST = registerBlock("golden_trapped_chest",
            () -> new GoldenTrappedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).sound(SoundType.WOOD)));

    private static Supplier<Block> registerBlock(String name, Supplier<Block> block) {
        Supplier<Block> toReturn = Services.PLATFORM.register(BuiltInRegistries.BLOCK, name, block);
        ALL.add(toReturn);
        ModItems.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static void init() {
    }
}
