package net.luckyowlstudios.locksmith.block.chest.iron_trapped;

import com.mojang.serialization.MapCodec;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class IronTrappedChestBlock extends ChestBlock {

    public static final MapCodec<IronTrappedChestBlock> CODEC = simpleCodec(IronTrappedChestBlock::new);

    public MapCodec<IronTrappedChestBlock> codec() {
        return CODEC;
    }

    public IronTrappedChestBlock(BlockBehaviour.Properties properties) {
        super(properties, ModBlockEntityTypes.IRON_TRAPPED_CHEST_BLOCK_ENTITY::get);
    }

    protected Stat<ResourceLocation> getOpenChestStat() {
        return Stats.CUSTOM.get(Stats.TRIGGER_TRAPPED_CHEST);
    }

    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return Mth.clamp(ChestBlockEntity.getOpenCount(blockAccess, pos), 0, 15);
    }

    protected int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return side == Direction.UP ? blockState.getSignal(blockAccess, pos, side) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronTrappedChestBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            if (!(level.getBlockEntity(pos) instanceof IronTrappedChestBlockEntity containerBlockEntity)) return ItemInteractionResult.CONSUME_PARTIAL;
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
