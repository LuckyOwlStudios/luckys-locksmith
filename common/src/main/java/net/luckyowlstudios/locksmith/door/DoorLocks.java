package net.luckyowlstudios.locksmith.door;

import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where door and trapdoor locks are kept.
 * <p>
 * On the server the locks are saved per level in {@link DoorLockData}. Clients get a copy of the
 * locks in the chunks they can see through {@link DoorLockSyncPayload}, so a click on a locked door
 * is predicted correctly instead of the door flickering open. Both halves of a door share the lock
 * stored at the lower half.
 */
public final class DoorLocks {

    /** Client copies, per client level. Only touched on the client thread. */
    private static final Map<Level, DoorLockMap> CLIENT_LOCKS = new WeakHashMap<>();

    private DoorLocks() {
    }

    public static boolean isLockable(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock;
    }

    /** The position a door or trapdoor's lock is stored at: the lower half for doors. */
    public static BlockPos lockPos(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    @Nullable
    public static DoorLock get(Level level, BlockPos pos) {
        return get(level, level.getBlockState(pos), pos);
    }

    /** @return the lock on the door or trapdoor {@code state} at {@code pos}, or null if it has none. */
    @Nullable
    public static DoorLock get(Level level, BlockState state, BlockPos pos) {
        if (!isLockable(state)) return null;
        DoorLockMap locks = level instanceof ServerLevel serverLevel ? DoorLockData.get(serverLevel).locks() : CLIENT_LOCKS.get(level);
        return locks == null ? null : locks.get(lockPos(state, pos));
    }

    public static boolean isLocked(Level level, BlockState state, BlockPos pos) {
        return get(level, state, pos) != null;
    }

    /** Puts {@code lock} on the door or trapdoor at {@code pos}, or removes its lock if null. */
    public static void set(ServerLevel level, BlockState state, BlockPos pos, @Nullable DoorLock lock) {
        DoorLockData data = DoorLockData.get(level);
        BlockPos key = lockPos(state, pos);
        if (lock == null) {
            data.locks().remove(key);
        } else {
            data.locks().put(key, lock);
        }
        data.setDirty();

        // Tell everyone who can see the chunk
        ChunkPos chunkPos = new ChunkPos(key);
        DoorLockSyncPayload payload = new DoorLockSyncPayload(chunkPos.toLong(), data.locks().entries(chunkPos.toLong()));
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            Services.PLATFORM.sendToPlayer(player, payload);
        }
    }

    /** Drops the lock of a door or trapdoor that is broken or replaced by another block. */
    public static void onBlockRemoved(Level level, BlockState state, BlockPos pos, BlockState newState) {
        if (level instanceof ServerLevel serverLevel && isLockable(state) && !newState.is(state.getBlock())
                && get(serverLevel, state, pos) != null) {
            set(serverLevel, state, pos, null);
        }
    }

    /** Called right after a chunk is sent to {@code player}. */
    public static void onChunkSent(ServerPlayer player, ServerLevel level, ChunkPos chunkPos) {
        List<DoorLockSyncPayload.Entry> entries = DoorLockData.get(level).locks().entries(chunkPos.toLong());
        // A newly received chunk starts with no locks on the client, so empty chunks need no packet
        if (!entries.isEmpty()) {
            Services.PLATFORM.sendToPlayer(player, new DoorLockSyncPayload(chunkPos.toLong(), entries));
        }
    }

    // ========================================
    // CLIENT
    // ========================================

    public static void applyClientSync(Level clientLevel, DoorLockSyncPayload payload) {
        CLIENT_LOCKS.computeIfAbsent(clientLevel, level -> new DoorLockMap()).replaceChunk(payload.chunkPos(), payload.entries());
    }

    /** The client got a fresh copy of a chunk; forget its old locks until the server sends them. */
    public static void clearClientChunk(Level clientLevel, int chunkX, int chunkZ) {
        DoorLockMap locks = CLIENT_LOCKS.get(clientLevel);
        if (locks != null) {
            locks.clearChunk(ChunkPos.asLong(chunkX, chunkZ));
        }
    }
}
