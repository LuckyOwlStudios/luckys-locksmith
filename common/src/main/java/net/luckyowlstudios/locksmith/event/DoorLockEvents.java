package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.door.DoorLock;
import net.luckyowlstudios.locksmith.door.DoorLocks;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Lock logic for doors and trapdoors, which keep their locks in {@link DoorLocks} instead of a block
 * entity. Works like chests: right-click with a key to put a player lock on, with a golden or trial
 * lock to put that lock on, and a locked door only opens for someone carrying the right key.
 * <p>
 * Runs on both sides with the same data, so the client predicts the result and a locked door never
 * flickers open. Only the server changes locks, plays sounds and shows messages.
 */
final class DoorLockEvents {

    private DoorLockEvents() {
    }

    static LockEvents.ClickResult onRightClick(Player player, Level level, InteractionHand hand, BlockPos pos, BlockState state) {
        ItemStack heldItem = player.getMainHandItem();
        DoorLock lock = DoorLocks.get(level, state, pos);

        if (lock != null) {
            if (lock.isSpecial()) {
                if (isMatchingSpecialKey(lock.type(), heldItem)) {
                    if (level instanceof ServerLevel serverLevel) {
                        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                        level.playSound(null, pos, Blocks.CHAIN.defaultBlockState().getSoundType().getBreakSound(),
                                player.getSoundSource(), 1.0F, 1.0F);
                        DoorLocks.set(serverLevel, state, pos, null);
                        player.swing(hand, true);
                        heldItem.shrink(1);
                    }
                    return LockEvents.ClickResult.CANCEL;
                }
            } else if (LockEvents.hasKeyFor(player, lock.code())) {
                if (!level.isClientSide()) {
                    level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                }
                // Let vanilla open it
                return LockEvents.ClickResult.PASS;
            }

            // Still lets players place blocks against a locked door while sneaking
            if (player.isCrouching() && heldItem.getItem() instanceof BlockItem) {
                return LockEvents.ClickResult.PASS;
            }
            if (!level.isClientSide()) {
                LockEvents.failedToOpen(player, level, pos);
                player.swing(hand, true);
                resyncOtherHalf(player, level, pos, state);
            }
            return LockEvents.ClickResult.CANCEL_FAIL;
        }

        // Not locked yet: lock it if holding a lock or key
        DoorLock newLock = lockFor(heldItem, player);
        if (newLock == null) {
            return LockEvents.ClickResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            DoorLocks.set(serverLevel, state, pos, newLock);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, newLock.isSpecial() ? 1.5F : 1.0F);
            if (!newLock.isSpecial()) {
                player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock",
                        state.getBlock().getName()).append(newLock.code()), true);
            }
            player.swing(hand, true);
            resyncOtherHalf(player, level, pos, state);
        }
        return LockEvents.ClickResult.CANCEL;
    }

    static boolean canBreak(Player player, Level level, BlockPos pos, BlockState state) {
        DoorLock lock = DoorLocks.get(level, state, pos);
        if (lock == null || player.isCreative() || player.getUUID().equals(lock.owner())) {
            return true;
        }
        boolean hasKey = switch (lock.type()) {
            case GOLDEN -> LockEvents.hasItemInInventory(player, ModItems.GOLDEN_KEY.get());
            case TRIAL -> LockEvents.hasItemInInventory(player, Items.TRIAL_KEY);
            default -> LockEvents.hasKeyFor(player, lock.code());
        };
        if (!hasKey) {
            LockEvents.failedToOpen(player, level, pos);
            // The client already removed both halves; the loader only sends back the clicked one
            resyncOtherHalf(player, level, pos, state);
        }
        return hasKey;
    }

    private static boolean isMatchingSpecialKey(LockType lockType, ItemStack heldItem) {
        return (lockType == LockType.GOLDEN && heldItem.is(ModItems.GOLDEN_KEY.get()))
                || (lockType == LockType.TRIAL && heldItem.is(Items.TRIAL_KEY));
    }

    /** @return the lock that clicking an unlocked door with {@code heldItem} puts on, or null for none. */
    private static DoorLock lockFor(ItemStack heldItem, Player player) {
        if (heldItem.is(ModItems.GOLDEN_LOCK.get())) {
            return new DoorLock(LockType.GOLDEN, "", player.getUUID());
        }
        if (heldItem.is(ModItems.TRIAL_LOCK.get())) {
            return new DoorLock(LockType.TRIAL, "", player.getUUID());
        }
        if (heldItem.getItem() instanceof KeyItem && heldItem.has(DataComponents.LOCK)) {
            return new DoorLock(LockType.LOCK, heldItem.get(DataComponents.LOCK).key(), player.getUUID());
        }
        return null;
    }

    /** Vanilla only resends the clicked block, so a client that guessed wrong could show half a door. */
    private static void resyncOtherHalf(Player player, Level level, BlockPos pos, BlockState state) {
        if (player instanceof ServerPlayer serverPlayer && state.getBlock() instanceof DoorBlock) {
            BlockPos otherHalf = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, otherHalf));
        }
    }
}
