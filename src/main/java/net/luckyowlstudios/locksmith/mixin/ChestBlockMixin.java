package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.function.Supplier;

// There is a bug where players can just place a chest next to an existing LOCKED chest and convert it to a double chest, which than allows them to open locked chest. This stops this from happening!
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin extends AbstractChestBlock<ChestBlockEntity> implements SimpleWaterloggedBlock {

    protected ChestBlockMixin(Properties properties, Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType) {
        super(properties, blockEntityType);
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void locksmith$updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos, CallbackInfoReturnable<BlockState> cir) {
        if (level.getBlockEntity(currentPos) instanceof BaseContainerBlockEntity baseContainerBlockEntity && state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            if (baseContainerBlockEntity.components().has(DataComponents.LOCK) || baseContainerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
                cir.setReturnValue(state);
            }
        }
    }

    @Inject(method = "getStateForPlacement", at = @At(value = "TAIL"), cancellable = true)
    private void locksmith$modifyPlacementToPreventConnectingToLockedChest(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        if (cir.getReturnValue().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            boolean flag = context.isSecondaryUseActive();
            if (!flag) {
                if (direction == this.candidatePartnerFacing(context, direction.getClockWise())) {
                    cir.setReturnValue(cir.getReturnValue().setValue(ChestBlock.TYPE, ChestType.LEFT));
                } else if (direction == this.candidatePartnerFacing(context, direction.getCounterClockWise())) {
                    cir.setReturnValue(cir.getReturnValue().setValue(ChestBlock.TYPE, ChestType.RIGHT));
                }
            }
        }
    }

    @Nullable
    private Direction candidatePartnerFacing(BlockPlaceContext context, Direction direction) {
        BlockPos blockPos = context.getClickedPos().relative(direction);
        BlockState blockstate = context.getLevel().getBlockState(blockPos);
        Level level = context.getLevel();
        if (level.getBlockEntity(blockPos) instanceof BaseContainerBlockEntity baseContainerBlockEntity) {
            if (baseContainerBlockEntity.components().has(DataComponents.LOCK) || baseContainerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
                return null; // Prevent connecting to locked chests
            }
        }
        return blockstate.is(this) && blockstate.getValue(ChestBlock.TYPE) == ChestType.SINGLE ? blockstate.getValue(ChestBlock.FACING) : null;
    }

}
