package net.luckyowlstudios.locksmith.block.chest.iron;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IronChestBlockEntity extends ChestBlockEntity {

    protected IronChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public IronChestBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntityTypes.IRON_CHEST_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.locksmith.iron_chest");
    }
}
