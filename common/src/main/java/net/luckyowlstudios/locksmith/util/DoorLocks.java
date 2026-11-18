package net.luckyowlstudios.locksmith.util;

import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.network.DoorLocksPayload;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Locks on doors and trapdoors. The server owns the locks ({@link DoorLockData}) and decides every
 * click; clients only get to know which doors are locked, to draw the padlock and to not open a
 * locked door on their own before the server answers.
 * <p>
 * Both halves of a door share the lock stored at the lower half.
 */
public final class DoorLocks {

    /** What each client knows: lock types by chunk. Only touched on the client thread. */
    private static final Map<Level, Map<Long, Map<BlockPos, LockType>>> CLIENT_LOCKS = new WeakHashMap<>();

    /** True while the server opens or closes a locked door for someone who has the key. */
    private static boolean opening;

    private DoorLocks() {
    }

    /** @return true if a door is being moved for a player with the key, so its lock must not stop it. */
    public static boolean isOpeningForKeyHolder() {
        return opening;
    }

    public static boolean isLockable(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock;
    }

    /** The position a door or trapdoor's lock is stored at: the lower half for doors. */
    public static BlockPos lowerPos(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** @return the full lock of a door or trapdoor, or null if it has none. Always null on the client. */
    @Nullable
    public static DoorLockData.Entry get(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return null;
        BlockState state = level.getBlockState(pos);
        if (!isLockable(state)) return null;
        return DoorLockData.get(server).get(lowerPos(pos, state));
    }

    /** @return the kind of lock on a door or trapdoor, or null if it has none. Works on both sides. */
    @Nullable
    public static LockType typeAt(Level level, BlockState state, BlockPos pos) {
        if (!isLockable(state)) return null;
        BlockPos lockPos = lowerPos(pos, state);
        if (level instanceof ServerLevel server) {
            DoorLockData.Entry entry = DoorLockData.get(server).get(lockPos);
            return entry == null ? null : entry.type();
        }
        Map<Long, Map<BlockPos, LockType>> chunks = CLIENT_LOCKS.get(level);
        Map<BlockPos, LockType> chunk = chunks == null ? null : chunks.get(ChunkPos.asLong(lockPos));
        return chunk == null ? null : chunk.get(lockPos);
    }

    public static boolean isLocked(Level level, BlockState state, BlockPos pos) {
        return typeAt(level, state, pos) != null;
    }

    public static boolean isLocked(Level level, BlockPos pos) {
        return isLocked(level, level.getBlockState(pos), pos);
    }

    // ========================================
    // KEYS
    // ========================================

    public static boolean matches(ItemStack stack, String code) {
        LockCode key = stack.get(DataComponents.LOCK);
        return stack.getItem() instanceof KeyItem && key != null && key.key().equals(code);
    }

    /** @return true if the player carries what opens this lock: in hand, in the inventory or worn. */
    public static boolean hasKey(Player player, DoorLockData.Entry lock) {
        if (lock.type() != LockType.LOCK) {
            return player.getInventory().hasAnyMatching(stack -> specialKey(stack, lock.type()));
        }
        if (player.getInventory().hasAnyMatching(stack -> matches(stack, lock.keyCode()))) return true;
        return Services.PLATFORM.findEquippedKey(player).filter(stack -> matches(stack, lock.keyCode())).isPresent();
    }

    public static boolean specialKey(ItemStack stack, LockType type) {
        return type == LockType.GOLDEN && stack.is(ModItems.GOLDEN_KEY.get())
                || type == LockType.TRIAL && stack.is(Items.TRIAL_KEY);
    }

    // ========================================
    // CLICKING
    // ========================================

    /**
     * Right-click on a door or trapdoor with the main hand. The server answers PASS for someone with the
     * key, so vanilla opens the door as usual; the client can't tell and waits for the server.
     */
    public static LockEvents.ClickResult onRightClick(Player player, Level level, InteractionHand hand, BlockPos clickedPos, BlockState clicked) {
        ItemStack held = player.getItemInHand(hand);
        BlockPos pos = lowerPos(clickedPos, clicked);
        LockType visibleType = typeAt(level, clicked, clickedPos);

        if (visibleType != null) {
            // Still lets players place blocks against a locked door while sneaking
            if (player.isCrouching() && held.getItem() instanceof BlockItem) return LockEvents.ClickResult.PASS;
            if (!(level instanceof ServerLevel server)) return LockEvents.ClickResult.CANCEL;

            DoorLockData data = DoorLockData.get(server);
            DoorLockData.Entry lock = data.get(pos);
            if (lock == null) return LockEvents.ClickResult.PASS;
            if (lock.type() == LockType.LOCK && hasKey(player, lock)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                if (level.getBlockState(pos).getBlock() instanceof DoorBlock door) {
                    // The client waits for the server, so it never opened the door itself and vanilla would
                    // leave out the opening sound for this player. Move the door with no entity so everyone
                    // hears it, and send both halves together so they don't open a frame apart.
                    BlockState lower = level.getBlockState(pos);
                    opening = true;
                    try {
                        door.setOpen(null, level, lower, pos, !lower.getValue(DoorBlock.OPEN));
                    } finally {
                        opening = false;
                    }
                    correctPrediction(player, level, pos, lower);
                    return LockEvents.ClickResult.CANCEL;
                }
                return LockEvents.ClickResult.PASS;
            }
            if (specialKey(held, lock.type())) {
                // As with chest special locks, unlocking consumes the key and removes the lock.
                data.remove(pos);
                held.shrink(1);
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                level.playSound(null, pos, Blocks.CHAIN.defaultBlockState().getSoundType().getBreakSound(),
                        player.getSoundSource(), 1.0F, 1.0F);
                player.swing(hand, true);
                return LockEvents.ClickResult.CANCEL;
            }
            failed(player, level, pos);
            player.swing(hand, true);
            correctPrediction(player, level, pos, clicked);
            return LockEvents.ClickResult.CANCEL_FAIL;
        }

        // Not locked: a lock or key in hand locks it
        LockType type;
        String code = "";
        if (held.is(ModItems.GOLDEN_LOCK.get())) type = LockType.GOLDEN;
        else if (held.is(ModItems.TRIAL_LOCK.get())) type = LockType.TRIAL;
        else if (held.getItem() instanceof KeyItem) {
            LockCode key = held.get(DataComponents.LOCK);
            if (key == null || key.key().isEmpty()) return LockEvents.ClickResult.CANCEL;
            type = LockType.LOCK;
            code = key.key();
        } else return LockEvents.ClickResult.PASS;
        if (!(level instanceof ServerLevel server)) return LockEvents.ClickResult.CANCEL;

        DoorLockData.get(server).put(pos, new DoorLockData.Entry(type, code, player.getUUID()));
        closeAndUnpower(server, pos, clicked);
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, type == LockType.LOCK ? 1.0F : 1.5F);
        if (type == LockType.LOCK) {
            player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", clicked.getBlock().getName()).append(code), true);
        }
        player.swing(hand, true);
        correctPrediction(player, level, pos, clicked);
        return LockEvents.ClickResult.CANCEL;
    }

    /** A lock shuts the door, and old redstone power is forgotten so redstone works normally once it is removed. */
    private static void closeAndUnpower(ServerLevel level, BlockPos lockPos, BlockState clicked) {
        if (clicked.getBlock() instanceof DoorBlock) {
            for (BlockPos half : new BlockPos[]{lockPos, lockPos.above()}) {
                BlockState state = level.getBlockState(half);
                if (state.getBlock() instanceof DoorBlock) {
                    level.setBlock(half, state.setValue(DoorBlock.OPEN, false).setValue(DoorBlock.POWERED, false), 2);
                }
            }
        } else if (clicked.getBlock() instanceof TrapDoorBlock) {
            level.setBlock(lockPos, level.getBlockState(lockPos).setValue(TrapDoorBlock.OPEN, false).setValue(TrapDoorBlock.POWERED, false), 2);
        }
    }

    // ========================================
    // BREAKING
    // ========================================

    /**
     * The block holding a locked door in place counts as the door, otherwise breaking the block
     * under a locked door would drop it.
     * @return the position of the locked door or trapdoor that {@code pos} is or supports, or null
     */
    @Nullable
    public static BlockPos protectedDoor(Level level, BlockPos pos) {
        if (isLocked(level, pos)) return pos;
        if (isLocked(level, pos.above())) return pos.above();
        return null;
    }

    /** @return false if the player may not break the block at {@code pos} because of a door lock. Server only. */
    public static boolean canBreak(Player player, Level level, BlockPos pos) {
        BlockPos door = protectedDoor(level, pos);
        if (door == null) return true;
        DoorLockData.Entry lock = get(level, door);
        if (lock == null || player.isCreative() || player.getUUID().equals(lock.owner()) || hasKey(player, lock)) return true;
        failed(player, level, door);
        if (player instanceof ServerPlayer serverPlayer) {
            // The client already removed both halves; resend them
            BlockState state = level.getBlockState(door);
            correctPrediction(serverPlayer, level, lowerPos(door, state), state);
        }
        return false;
    }

    /** Drops the lock of a door or trapdoor that is broken. A copper door that weathers keeps its lock. */
    public static void onBlockRemoved(Level level, BlockState state, BlockPos pos, BlockState newState) {
        if (!(level instanceof ServerLevel server) || !isLockable(state)) return;
        boolean sameKind = state.getBlock() instanceof DoorBlock ? newState.getBlock() instanceof DoorBlock
                : newState.getBlock() instanceof TrapDoorBlock;
        if (!sameKind) {
            DoorLockData.get(server).remove(lowerPos(pos, state));
        }
    }

    // ========================================
    // MESSAGES AND SYNC
    // ========================================

    public static void failed(Player player, Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", level.getBlockState(pos).getBlock().getName()), true);
    }

    /** Vanilla only resends the clicked block, so resend both halves in case the client guessed wrong. */
    private static void correctPrediction(Player player, Level level, BlockPos lockPos, BlockState state) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, lockPos));
            if (state.getBlock() instanceof DoorBlock) {
                serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, lockPos.above()));
            }
        }
    }

    /** Sends everyone who can see the chunk the current locks in it. */
    public static void syncChunk(ServerLevel level, ChunkPos chunk) {
        DoorLocksPayload payload = snapshot(level, chunk);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunk, false)) {
            Services.PLATFORM.sendToPlayer(player, payload);
        }
    }

    public static DoorLocksPayload snapshot(ServerLevel level, ChunkPos chunk) {
        return new DoorLocksPayload(level.dimension().location(), chunk.toLong(), DoorLockData.get(level).visuals(chunk));
    }

    /** Called right after a chunk is sent to {@code player}. */
    public static void onChunkSent(ServerPlayer player, ServerLevel level, ChunkPos chunk) {
        DoorLocksPayload payload = snapshot(level, chunk);
        // A newly received chunk starts with no locks on the client, so an empty chunk needs no packet
        if (!payload.locks().isEmpty()) {
            Services.PLATFORM.sendToPlayer(player, payload);
        }
    }

    // ========================================
    // CLIENT
    // ========================================

    public static void applyClientSync(Level clientLevel, DoorLocksPayload payload) {
        if (!clientLevel.dimension().location().equals(payload.dimension())) return;
        Map<Long, Map<BlockPos, LockType>> chunks = CLIENT_LOCKS.computeIfAbsent(clientLevel, level -> new HashMap<>());
        if (payload.locks().isEmpty()) chunks.remove(payload.chunk());
        else chunks.put(payload.chunk(), payload.locks());
    }

    /** The client got a fresh copy of a chunk, or forgot it; its old locks are only valid until the server sends new ones. */
    public static void clearClientChunk(Level clientLevel, int chunkX, int chunkZ) {
        Map<Long, Map<BlockPos, LockType>> chunks = CLIENT_LOCKS.get(clientLevel);
        if (chunks != null) chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    /** Every lock the client knows of in the level, for drawing. */
    public static Iterable<Map<BlockPos, LockType>> clientChunks(Level clientLevel) {
        Map<Long, Map<BlockPos, LockType>> chunks = CLIENT_LOCKS.get(clientLevel);
        return chunks == null ? java.util.List.of() : chunks.values();
    }
}
