package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.DoorLockData;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin extends Block {
    protected DoorBlockMixin(Properties properties) { super(properties); }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void locksmith$blockRedstone(BlockState state, Level level, BlockPos pos, Block block,
                                         BlockPos fromPos, boolean moving, CallbackInfo ci) {
        if (DoorLocks.get(level, pos) != null) ci.cancel();
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void locksmith$blockAutomaticOpening(Entity entity, Level level, BlockState state,
                                                  BlockPos pos, boolean open, CallbackInfo ci) {
        DoorLockData.Entry lock = DoorLocks.get(level, pos);
        if (open && lock != null && !(entity instanceof Player player
                && lock.type() == LockType.LOCK && DoorLocks.hasKey(player, lock))) ci.cancel();
    }

    // DoorBlock inherits this method. Override it instead of injecting into a nonexistent method.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        // Copper weathering changes the block identity but must retain its lock.
        if (!(newState.getBlock() instanceof DoorBlock) && level instanceof ServerLevel server) {
            DoorLockData.get(server).remove(DoorLocks.lowerPos(pos, state));
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
