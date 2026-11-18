package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pistons would otherwise break locked doors (and push locked trapdoors away from their lock).
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void locksmith$lockedDoorsAreImmovable(BlockState state, Level level, BlockPos pos, Direction movementDirection,
                                                          boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (DoorLocks.isLocked(level, state, pos)) {
            cir.setReturnValue(false);
        }
    }
}
