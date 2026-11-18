package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds dungeon slimes to the hostile mobs that can spawn inside strongholds. Vanilla uses this list
 * both to pick a mob and to check the pick is still allowed, so adding to it here covers both.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void locksmith$addStrongholdSpawns(ServerLevel level, StructureManager structureManager, ChunkGenerator generator,
                                                      MobCategory category, BlockPos pos, @Nullable Holder<Biome> biome,
                                                      CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        if (category != MobCategory.MONSTER || !DungeonSlime.isInStronghold(structureManager, pos)) return;
        cir.setReturnValue(DungeonSlime.withStrongholdSpawn(cir.getReturnValue()));
    }
}
