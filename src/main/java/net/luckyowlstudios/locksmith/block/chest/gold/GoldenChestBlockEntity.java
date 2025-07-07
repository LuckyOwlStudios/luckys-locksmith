package net.luckyowlstudios.locksmith.block.chest.gold;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GoldenChestBlockEntity extends ChestBlockEntity {

    protected GoldenChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public GoldenChestBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntityTypes.GOLDEN_CHEST_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.locksmith.golden_chest");
    }
}
