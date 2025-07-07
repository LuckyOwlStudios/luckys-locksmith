package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;

public interface ModDamageTypes  {
    ResourceKey<DamageType> CURSED_LOCK = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "cursed_lock"));

    static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(
                CURSED_LOCK, new DamageType("cursed_lock", DamageScaling.NEVER, 0.0F, DamageEffects.HURT, DeathMessageType.DEFAULT)
        );
    }
}
