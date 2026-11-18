package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.DoorLockData;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
 * A locked door ignores redstone, and only a player with the key can open it: {@code setOpen} is also
 * how villagers and wind charges move doors.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void locksmith$ignoreRedstoneWhenLocked(BlockState state, Level level, BlockPos pos, Block block,
                                                    BlockPos fromPos, boolean movedByPiston, CallbackInfo ci) {
        if (DoorLocks.isLocked(level, state, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void locksmith$blockAutomaticOpening(@Nullable Entity entity, Level level, BlockState state,
                                                 BlockPos pos, boolean open, CallbackInfo ci) {
        // Only the server knows the keys, and only the server decides
        DoorLockData.Entry lock = DoorLocks.get(level, pos);
        if (open && lock != null && !(entity instanceof Player player
                && lock.type() == LockType.LOCK && DoorLocks.hasKey(player, lock))) {
            ci.cancel();
        }
    }
}
