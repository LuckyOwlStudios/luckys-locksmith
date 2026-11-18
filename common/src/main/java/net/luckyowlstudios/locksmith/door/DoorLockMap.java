package net.luckyowlstudios.locksmith.door;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Door locks of one level, grouped by chunk so a chunk's locks can be sent to a client together.
 * Keys are always the lock position from {@link DoorLocks#lockPos}.
 */
public class DoorLockMap {

    private final Long2ObjectMap<Long2ObjectMap<DoorLock>> byChunk = new Long2ObjectOpenHashMap<>();

    @Nullable
    public DoorLock get(BlockPos pos) {
        Long2ObjectMap<DoorLock> chunk = byChunk.get(ChunkPos.asLong(pos));
        return chunk == null ? null : chunk.get(pos.asLong());
    }

    public void put(BlockPos pos, DoorLock lock) {
        byChunk.computeIfAbsent(ChunkPos.asLong(pos), key -> new Long2ObjectOpenHashMap<>()).put(pos.asLong(), lock);
    }

    public void remove(BlockPos pos) {
        long chunkKey = ChunkPos.asLong(pos);
        Long2ObjectMap<DoorLock> chunk = byChunk.get(chunkKey);
        if (chunk != null) {
            chunk.remove(pos.asLong());
            if (chunk.isEmpty()) {
                byChunk.remove(chunkKey);
            }
        }
    }

    public List<DoorLockSyncPayload.Entry> entries(long chunkPos) {
        Long2ObjectMap<DoorLock> chunk = byChunk.get(chunkPos);
        List<DoorLockSyncPayload.Entry> entries = new ArrayList<>();
        if (chunk != null) {
            chunk.long2ObjectEntrySet().forEach(entry ->
                    entries.add(new DoorLockSyncPayload.Entry(BlockPos.of(entry.getLongKey()), entry.getValue())));
        }
        return entries;
    }

    public void replaceChunk(long chunkPos, List<DoorLockSyncPayload.Entry> entries) {
        byChunk.remove(chunkPos);
        entries.forEach(entry -> put(entry.pos(), entry.lock()));
    }

    public void clearChunk(long chunkPos) {
        byChunk.remove(chunkPos);
    }

    public void forEach(BiConsumer<BlockPos, DoorLock> action) {
        byChunk.values().forEach(chunk -> chunk.long2ObjectEntrySet().forEach(entry ->
                action.accept(BlockPos.of(entry.getLongKey()), entry.getValue())));
    }
}
