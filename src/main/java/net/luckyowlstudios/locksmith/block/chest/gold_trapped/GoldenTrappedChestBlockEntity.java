package net.luckyowlstudios.locksmith.block.chest.gold_trapped;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GoldenTrappedChestBlockEntity extends ChestBlockEntity {

    public GoldenTrappedChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntityTypes.GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.locksmith.golden_chest");
    }

    protected void signalOpenCount(Level p_155865_, BlockPos p_155866_, BlockState p_155867_, int p_155868_, int p_155869_) {
        super.signalOpenCount(p_155865_, p_155866_, p_155867_, p_155868_, p_155869_);
        if (p_155868_ != p_155869_) {
            Block block = p_155867_.getBlock();
            p_155865_.updateNeighborsAt(p_155866_, block);
            p_155865_.updateNeighborsAt(p_155866_.below(), block);
        }

    }
}
