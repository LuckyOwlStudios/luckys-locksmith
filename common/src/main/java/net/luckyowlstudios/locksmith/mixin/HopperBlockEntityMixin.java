package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.LockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hoppers and hopper minecarts can't pull items out of a locked container. Putting items in is still allowed.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "getSourceContainer", at = @At("HEAD"), cancellable = true)
    private static void locksmith$noPullingFromLockedContainers(Level level, Hopper hopper, BlockPos pos,
                                                                BlockState state, CallbackInfoReturnable<Container> cir) {
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container && LockHandler.containerHasLock(container)) {
            cir.setReturnValue(null);
        }
    }
}
