package net.luckyowlstudios.locksmith.fabric.mixin;

import net.luckyowlstudios.locksmith.event.LockEvents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps golden and trial locked containers from being blown up (NeoForge does this with its
 * explosion event). Runs before the explosion destroys its blocks.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow
    @Final
    private Level level;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void locksmith$protectLockedBlocks(boolean spawnParticles, CallbackInfo ci) {
        LockEvents.protectLockedBlocksFromExplosion(this.level, ((Explosion) (Object) this).getToBlow());
    }
}
