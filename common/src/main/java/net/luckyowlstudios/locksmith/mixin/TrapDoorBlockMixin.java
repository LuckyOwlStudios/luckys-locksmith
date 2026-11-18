package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.door.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * A locked trapdoor ignores redstone and wind charges.
 */
@Mixin(TrapDoorBlock.class)
public abstract class TrapDoorBlockMixin {

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void locksmith$ignoreRedstoneWhenLocked(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                                    BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        if (DoorLocks.isLocked(level, state, pos)) {
            ci.cancel();
        }
    }

    // Explosions that trigger blocks (wind charges) toggle trapdoors here and never destroy anything,
    // so skipping the whole method only skips the toggle
    @Inject(method = "onExplosionHit", at = @At("HEAD"), cancellable = true)
    private void locksmith$ignoreWindChargesWhenLocked(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                                       BiConsumer<ItemStack, BlockPos> dropConsumer, CallbackInfo ci) {
        if (explosion.canTriggerBlocks() && DoorLocks.isLocked(level, state, pos)) {
            ci.cancel();
        }
    }
}
