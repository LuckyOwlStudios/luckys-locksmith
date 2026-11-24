package net.luckyowlstudios.locksmith.block.chest.jeweled;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class JeweledChestBlockEntity extends ChestBlockEntity {

    protected JeweledChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public JeweledChestBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntityTypes.JEWELED_CHEST_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.locksmith.jeweled_chest");
    }
}
