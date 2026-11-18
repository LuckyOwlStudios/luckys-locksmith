package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zombies and other mobs that break doors can't break a locked one.
 */
@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin extends DoorInteractGoal {

    protected BreakDoorGoalMixin(Mob mob) {
        super(mob);
    }

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void locksmith$leaveLockedDoorsAlone(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && DoorLocks.isLocked(this.mob.level(), this.doorPos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("RETURN"), cancellable = true)
    private void locksmith$stopWhenDoorGetsLocked(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && DoorLocks.isLocked(this.mob.level(), this.doorPos)) {
            cir.setReturnValue(false);
        }
    }
}
