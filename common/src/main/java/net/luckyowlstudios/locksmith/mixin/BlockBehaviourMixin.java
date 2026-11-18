package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forgets a door or trapdoor's lock once the block is gone, so a new door placed there isn't locked.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void locksmith$forgetDoorLock(BlockState state, Level level, BlockPos pos, BlockState newState,
                                          boolean movedByPiston, CallbackInfo ci) {
        DoorLocks.onBlockRemoved(level, state, pos, newState);
    }
}
