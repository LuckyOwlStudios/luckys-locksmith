package net.luckyowlstudios.locksmith.datagen.blocks;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Locksmith.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag((BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
                .add(ModBlocks.IRON_CHEST.get())
                .add(ModBlocks.GOLDEN_CHEST.get())
                .add(ModBlocks.IRON_TRAPPED_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;

        tag(Tags.Blocks.CHESTS)
                .add(ModBlocks.IRON_CHEST.get())
                .add(ModBlocks.GOLDEN_CHEST.get())
        ;

        tag(Tags.Blocks.CHESTS_TRAPPED)
                .add(ModBlocks.IRON_TRAPPED_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;

        tag(BlockTags.FEATURES_CANNOT_REPLACE)
                .add(ModBlocks.IRON_CHEST.get())
                .add(ModBlocks.GOLDEN_CHEST.get())
                .add(ModBlocks.IRON_TRAPPED_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.IRON_CHEST.get())
                .add(ModBlocks.GOLDEN_CHEST.get())
                .add(ModBlocks.IRON_TRAPPED_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.IRON_CHEST.get())
                .add(ModBlocks.GOLDEN_CHEST.get())
                .add(ModBlocks.IRON_TRAPPED_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;

        tag(BlockTags.GUARDED_BY_PIGLINS)
                .add(ModBlocks.GOLDEN_CHEST.get())
                .add(ModBlocks.GOLDEN_TRAPPED_CHEST.get())
        ;
    }
}
