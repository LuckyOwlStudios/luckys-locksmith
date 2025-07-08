package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.LockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * There is a bug where players can just place a chest next to an existing LOCKED chest and convert it to a double chest, which than allows them to open locked chest. This stops this from happening!
 */
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin extends AbstractChestBlock<ChestBlockEntity> {

    protected ChestBlockMixin(Properties properties, Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType) {
        super(properties, blockEntityType);
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void locksmith$updateShapeToPreventConnectingToLockedChest(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos, CallbackInfoReturnable<BlockState> cir) {
        if (level.getBlockEntity(currentPos) instanceof BaseContainerBlockEntity baseContainerBlockEntity && state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            if (LockHandler.containerHasLock(baseContainerBlockEntity)) {
                cir.setReturnValue(state);
            }
        }
    }

    @Inject(method = "candidatePartnerFacing", at = @At(value = "HEAD"), cancellable = true)
    private void locksmith$modifyCandidatePartnerFacing(BlockPlaceContext context, Direction direction, CallbackInfoReturnable<Direction> cir) {
        BlockPos blockPos = context.getClickedPos().relative(direction);
        Level level = context.getLevel();
        if (level.getBlockEntity(blockPos) instanceof BaseContainerBlockEntity baseContainerBlockEntity) {
            if (LockHandler.containerHasLock(baseContainerBlockEntity)) {
                cir.setReturnValue(null);
            }
        }
    }
}
