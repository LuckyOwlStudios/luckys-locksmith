package net.luckyowlstudios.locksmith.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves the locks of the doors and trapdoors of one level. Doors and trapdoors have no block entity
 * to keep a lock in like chests do. One entry per door, at its lower half; trapdoors at their own position.
 */
public final class DoorLockData extends SavedData {
    // Vanilla needs a data fixer type to load the file. No LEVEL fixer touches this data, so it never changes it.
    private static final Factory<DoorLockData> FACTORY = new Factory<>(DoorLockData::new, DoorLockData::load, DataFixTypes.LEVEL);
    private final Map<BlockPos, Entry> locks = new HashMap<>();
    private ServerLevel level;

    public record Entry(LockType type, String keyCode, UUID owner) {}

    public static DoorLockData get(ServerLevel level) {
        DoorLockData data = level.getDataStorage().computeIfAbsent(FACTORY, "locksmith_door_locks");
        data.level = level;
        return data;
    }

    @Nullable
    public Entry get(BlockPos lockPos) {
        return locks.get(lockPos);
    }

    public void put(BlockPos lockPos, Entry entry) {
        locks.put(lockPos.immutable(), entry);
        setDirty();
        if (level != null) DoorLocks.syncChunk(level, new ChunkPos(lockPos));
    }

    public void remove(BlockPos lockPos) {
        if (locks.remove(lockPos) != null) {
            setDirty();
            if (level != null) DoorLocks.syncChunk(level, new ChunkPos(lockPos));
        }
    }

    /** What clients get to know: which doors are locked and with what kind of lock, never key codes or owners. */
    public Map<BlockPos, LockType> visuals(ChunkPos chunk) {
        Map<BlockPos, LockType> result = new HashMap<>();
        locks.forEach((pos, lock) -> {
            if (new ChunkPos(pos).equals(chunk)) result.put(pos, lock.type());
        });
        return result;
    }

    public static DoorLockData load(CompoundTag tag, HolderLookup.Provider registries) {
        DoorLockData data = new DoorLockData();
        ListTag entries = tag.getList("Doors", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            LockType type;
            try {
                type = LockType.valueOf(entry.getString("Type"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (type == LockType.NONE || !entry.hasUUID("Owner")) continue;
            data.locks.put(BlockPos.of(entry.getLong("Pos")),
                    new Entry(type, entry.getString("Key"), entry.getUUID("Owner")));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag entries = new ListTag();
        locks.forEach((pos, lock) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putString("Type", lock.type().name());
            entry.putString("Key", lock.keyCode());
            entry.putUUID("Owner", lock.owner());
            entries.add(entry);
        });
        tag.put("Doors", entries);
        return tag;
    }
}
