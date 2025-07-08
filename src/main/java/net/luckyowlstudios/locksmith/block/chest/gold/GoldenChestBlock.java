package net.luckyowlstudios.locksmith.block.chest.gold;

import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GoldenChestBlock extends ChestBlock {

    public GoldenChestBlock(Properties properties) {
        super(properties, ModBlockEntityTypes.GOLDEN_CHEST_BLOCK_ENTITY::get);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenChestBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            if (!(level.getBlockEntity(pos) instanceof GoldenChestBlockEntity containerBlockEntity)) {
                return ItemInteractionResult.CONSUME_PARTIAL;
            }
            DataComponentMap dyeComponents = DataComponentMap.builder()
                    .addAll(containerBlockEntity.components())
                    .set(DataComponents.DYED_COLOR, new DyedItemColor(dyeItem.getDyeColor().getTextureDiffuseColor(), true))
                    .build();
            containerBlockEntity.setComponents(dyeComponents);
            containerBlockEntity.setChanged();
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
