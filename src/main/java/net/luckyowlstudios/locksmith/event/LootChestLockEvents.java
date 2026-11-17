package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.LocksmithConfig;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Locks some naturally generated loot chests with a golden lock.
 * <p>
 * When a chunk loads, every container that still has an unopened Nether fortress loot table and has
 * not been rolled yet gets a chance (see {@link LocksmithConfig#FORTRESS_CHEST_LOCK_CHANCE}) to be
 * locked with a golden lock. Containers that lose the roll are marked with {@link LockType#NONE} so
 * they are never rolled again, which keeps the chance fixed no matter how often the chunk reloads.
 * Because the roll happens on load rather than only on world generation, unopened fortress chests in
 * worlds created before this feature are covered too.
 */
@EventBusSubscriber(modid = Locksmith.MOD_ID)
public class LootChestLockEvents {

    private static final ResourceKey<LootTable> FORTRESS_LOOT = BuiltInLootTables.NETHER_BRIDGE;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        double chance = LocksmithConfig.FORTRESS_CHEST_LOCK_CHANCE.getAsDouble();
        boolean changed = false;

        // getBlockEntitiesPos() returns a copy that also covers block entities still waiting to be
        // created from saved data; getBlockEntity(pos) creates those on demand.
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) continue;
            if (!FORTRESS_LOOT.equals(container.getLootTable())) continue;
            // Already rolled, or a lock was put on it some other way.
            if (container.components().has(ModDataComponents.LOCK_TYPE.get())) continue;

            LockType result = chunk.getLevel().getRandom().nextDouble() < chance ? LockType.GOLDEN : LockType.NONE;
            container.setComponents(DataComponentMap.builder()
                    .addAll(container.components())
                    .set(ModDataComponents.LOCK_TYPE.get(), result)
                    .build());
            changed = true;
        }

        // Don't call setChanged() on the block entities here: it reaches back into the level for this
        // chunk while it is still loading. Marking the chunk unsaved is enough to persist the result.
        if (changed) {
            chunk.setUnsaved(true);
        }
    }
}
