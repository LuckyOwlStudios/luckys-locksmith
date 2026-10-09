package net.luckyowlstudios.locksmith.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ChunkPos;
import net.luckyowlstudios.locksmith.network.DoorLockNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One entry per door, at its lower half, in the dimension containing it. */
public final class DoorLockData extends SavedData {
    private static final Factory<DoorLockData> FACTORY = new Factory<>(DoorLockData::new, DoorLockData::load);
    private final Map<BlockPos, Entry> locks = new HashMap<>();
    private ServerLevel level;

    public record Entry(LockType type, String keyCode, UUID owner) {}

    public static DoorLockData get(ServerLevel level) {
        DoorLockData data = level.getDataStorage().computeIfAbsent(FACTORY, "locksmith_door_locks");
        data.level = level;
        return data;
    }

    public Entry get(BlockPos lowerPos) {
        return locks.get(lowerPos);
    }

    public void put(BlockPos lowerPos, Entry entry) {
        locks.put(lowerPos.immutable(), entry);
        setDirty();
        if (level != null) DoorLockNetwork.changed(level, lowerPos);
    }

    public void remove(BlockPos lowerPos) {
        if (locks.remove(lowerPos) != null) {
            setDirty();
            if (level != null) DoorLockNetwork.changed(level, lowerPos);
        }
    }

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
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
