package net.luckyowlstudios.locksmith.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * A gray slime found deep underground with a golden key floating inside it. It always spawns at the
 * medium size and splits into small dungeon slimes. Only the medium one carries the key, so it drops
 * exactly one golden key; see the {@code locksmith:entities/dungeon_slime} loot table.
 */
public class DungeonSlime extends Slime {

    /** The size the slime spawns at. Vanilla sizes are 1 (small), 2 (medium) and 4 (large). */
    public static final int SPAWN_SIZE = 2;
    /** Dungeon slimes only spawn below this height. */
    public static final int MAX_SPAWN_Y = 0;

    private static final ItemParticleOption PARTICLE = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GRAY_DYE));

    public DungeonSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    /** True while the slime still carries its golden key. */
    public boolean hasKey() {
        return this.getSize() >= SPAWN_SIZE;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        // Vanilla picks a random size, but there are no large dungeon slimes
        this.setSize(SPAWN_SIZE, true);
        return data;
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        // Vanilla slimes only use their loot table when small; the medium needs it to drop the key
        return this.getType().getDefaultLootTable();
    }

    @Override
    protected ParticleOptions getParticleType() {
        return PARTICLE;
    }

    @Override
    protected float getSoundPitch() {
        return super.getSoundPitch() * 0.85F;
    }

    public static boolean checkDungeonSlimeSpawnRules(EntityType<DungeonSlime> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (MobSpawnType.isSpawner(spawnType)) return checkMobSpawnRules(type, level, spawnType, pos, random);
        return pos.getY() < MAX_SPAWN_Y
                && !level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
                && !level.getBiome(pos).is(Biomes.DEEP_DARK)
                && Monster.isDarkEnoughToSpawn(level, pos, random)
                && checkMobSpawnRules(type, level, spawnType, pos, random);
    }
}
