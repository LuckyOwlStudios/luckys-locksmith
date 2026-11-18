package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.door.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A locked door ignores redstone, and nothing but a player with the key can open or close it:
 * {@code setOpen} is how villagers and wind charges move doors.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void locksmith$ignoreRedstoneWhenLocked(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                                    BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        if (DoorLocks.isLocked(level, state, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void locksmith$stayShutWhenLocked(@Nullable Entity entity, Level level, BlockState state, BlockPos pos,
                                              boolean open, CallbackInfo ci) {
        if (DoorLocks.isLocked(level, state, pos)) {
            ci.cancel();
        }
    }
}
