package net.luckyowlstudios.locksmith.door;

import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * Saves the door locks of one level in {@code <world>/<dimension>/data/locksmith_door_locks.dat}.
 * Doors and trapdoors have no block entity to keep a lock in like chests do, so the locks live here.
 */
public class DoorLockData extends SavedData {

    private static final String FILE_NAME = "locksmith_door_locks";
    // Vanilla needs a data fixer type to load the file. No LEVEL fixer touches this data, so it never changes it.
    private static final Factory<DoorLockData> FACTORY = new Factory<>(DoorLockData::new, DoorLockData::load, DataFixTypes.LEVEL);

    private final DoorLockMap locks = new DoorLockMap();

    public static DoorLockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    public DoorLockMap locks() {
        return locks;
    }

    private static DoorLockData load(CompoundTag tag, HolderLookup.Provider registries) {
        DoorLockData data = new DoorLockData();
        ListTag list = tag.getList("locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            LockType type = LockType.byName(entry.getString("type"));
            if (type == LockType.NONE) continue;
            data.locks.put(BlockPos.of(entry.getLong("pos")),
                    new DoorLock(type, entry.getString("code"), entry.hasUUID("owner") ? entry.getUUID("owner") : null));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        locks.forEach((pos, lock) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            entry.putString("type", lock.type().getSerializedName());
            entry.putString("code", lock.code());
            if (lock.owner() != null) {
                entry.putUUID("owner", lock.owner());
            }
            list.add(entry);
        });
        tag.put("locks", list);
        return tag;
    }
}
