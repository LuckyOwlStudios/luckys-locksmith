package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.platform.Services;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Loader-independent lock logic. Each loader module forwards its own events (or mixins, where the
 * loader has no event) to these methods.
 */
public final class LockEvents {

    private LockEvents() {
    }

    /** What the loader should do with a right-click on a block after Locksmith has looked at it. */
    public enum ClickResult {
        /** Let vanilla handle the click as usual. */
        PASS,
        /** Stop the click; Locksmith handled it. */
        CANCEL,
        /** Stop the click and report it as a failure. */
        CANCEL_FAIL
    }

    // ========================================
    // ANVIL - Key duplication system
    // ========================================

    /** A key in the left slot and an iron ingot in the right slot makes a copy of the key. */
    public static boolean isKeyDuplication(ItemStack left, ItemStack right) {
        return left.is(ModItems.KEY.get()) && right.is(Items.IRON_INGOT);
    }

    /** Called when the player takes the anvil output, to give back the original key as well. */
    public static void onKeyDuplicated(Player player, ItemStack left, ItemStack right) {
        if (isKeyDuplication(left, right)) {
            if (!player.getInventory().add(left.copy())) {
                player.drop(left.copy(), false);
            }
        }
    }

    // ========================================
    // TOOLTIPS
    // ========================================

    public static void appendTooltip(ItemStack itemStack, List<Component> tooltip) {
        if (itemStack.is(Items.TRIAL_KEY)) {
            tooltip.add(Component.translatable("tooltip.locksmith.trial_key").withStyle(ChatFormatting.GRAY));
        } else if (itemStack.is(Items.OMINOUS_TRIAL_KEY)) {
            tooltip.add(Component.translatable("tooltip.locksmith.ominous_trial_key").withStyle(ChatFormatting.GRAY));
        }
    }

    // ========================================
    // BLOCK BREAKING AND EXPLOSIONS
    // ========================================

    /** @return false if the player may not break the block at {@code pos}. */
    public static boolean canBreakBlock(Player player, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity) {
            if (LockHandler.containerHasLock(containerBlockEntity) && !player.isCreative()) {
                // Owner can always break their own locked chest
                UUID owner = containerBlockEntity.components().get(ModDataComponents.OWNER_UUID.get());
                if (owner != null && owner.equals(player.getUUID())) {
                    return true;
                }

                if (!playerHasCorrectKey(player, containerBlockEntity)) {
                    failedToOpen(player, level, pos);
                    return false;
                }
            }
        }
        return true;
    }

    /** Removes containers with a golden or trial lock from the list of blocks an explosion destroys. */
    public static void protectLockedBlocksFromExplosion(Level level, List<BlockPos> affectedBlocks) {
        // Only mod locks, not player locks
        affectedBlocks.removeIf(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseContainerBlockEntity container) {
                DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();
                return container.components().has(lockType) &&
                        container.components().get(lockType) != LockType.NONE;
            }
            return false;
        });
    }

    // ========================================
    // RIGHT CLICKING BLOCKS
    // ========================================

    public static ClickResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
        if (hand != InteractionHand.MAIN_HAND) return ClickResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity)) return ClickResult.PASS;

        ItemStack heldItem = player.getMainHandItem();

        // Handle special lock types (Golden/Trial)
        ClickResult special = handleSpecialLocks(containerBlockEntity, heldItem, player, level, pos, hand);
        if (special != null) {
            return special;
        }

        // Handle regular locks and key operations
        return handleRegularLocks(containerBlockEntity, heldItem, player, level, pos, hand);
    }

    /** @return null if the container has no golden or trial lock. */
    private static ClickResult handleSpecialLocks(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                                  Player player, Level level, BlockPos pos, InteractionHand hand) {
        DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();

        if (!containerBlockEntity.components().has(lockType)) {
            return null;
        }

        LockType currentLockType = containerBlockEntity.components().get(lockType);
        boolean hasCorrectKey = false;

        // Check for correct key based on lock type
        if (currentLockType == LockType.GOLDEN && heldItem.is(ModItems.GOLDEN_KEY.get())) {
            hasCorrectKey = true;
        } else if (currentLockType == LockType.TRIAL && heldItem.is(Items.TRIAL_KEY)) {
            hasCorrectKey = true;
        }

        if (hasCorrectKey) {
            // Unlock the container
            unlockContainer(containerBlockEntity, player, level, pos, hand, heldItem);
            return ClickResult.CANCEL;
        } else if (currentLockType != LockType.NONE) {
            // Wrong key or no key
            failedToOpen(player, level, pos);
            player.swing(hand, true);
            return ClickResult.CANCEL_FAIL;
        }

        return null;
    }

    private static ClickResult handleRegularLocks(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                                  Player player, Level level, BlockPos pos, InteractionHand hand) {
        boolean isHeldKey = heldItem.getItem() instanceof KeyItem;
        Optional<ItemStack> curiosKey = Services.PLATFORM.findEquippedKey(player);

        // Handle existing locks
        if (containerBlockEntity.components().has(DataComponents.LOCK)) {
            if (canUnlockWithKey(containerBlockEntity, heldItem, curiosKey, player)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                return ClickResult.PASS;
            } else if (!player.isCrouching() || !(heldItem.getItem() instanceof BlockItem)) {
                failedToOpen(player, level, pos);
                player.swing(hand, true);
                return ClickResult.CANCEL_FAIL;
            }
        }

        // Handle adding new locks
        if (canAddLock(containerBlockEntity)) {
            if (heldItem.is(ModItems.GOLDEN_LOCK.get())) {
                addLock(containerBlockEntity, LockType.GOLDEN, player, level, pos, hand);
                return ClickResult.CANCEL;
            } else if (heldItem.is(ModItems.TRIAL_LOCK.get())) {
                addLock(containerBlockEntity, LockType.TRIAL, player, level, pos, hand);
                return ClickResult.CANCEL;
            } else if (isHeldKey && !containerBlockEntity.components().has(DataComponents.LOCK)) {
                addPlayerLock(containerBlockEntity, heldItem, player, level, pos, hand);
                return ClickResult.CANCEL;
            }
        }
        return ClickResult.PASS;
    }

    private static void unlockContainer(BaseContainerBlockEntity containerBlockEntity, Player player,
                                        Level level, BlockPos pos, InteractionHand hand, ItemStack heldItem) {
        // Play unlock sounds
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
        BlockState chainState = Blocks.CHAIN.defaultBlockState();
        level.playSound(null, pos, chainState.getSoundType().getBreakSound(),
                player.getSoundSource(), 1.0F, 1.0F);

        // Remove lock
        DataComponentMap lockChanges = DataComponentMap.builder()
                .set(ModDataComponents.LOCK_TYPE.get(), LockType.NONE)
                .build();
        applyChangesToBlock(containerBlockEntity, lockChanges);

        player.swing(hand);
        heldItem.shrink(1);
    }

    private static boolean canUnlockWithKey(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                            Optional<ItemStack> curiosKey, Player player) {
        String containerKeyCode = containerBlockEntity.components().get(DataComponents.LOCK).key();

        // Check accessory slot key
        if (curiosKey.isPresent() && curiosKey.get().has(DataComponents.LOCK)) {
            String curiosKeyCode = curiosKey.get().get(DataComponents.LOCK).key();
            if (curiosKeyCode.equals(containerKeyCode)) {
                return true;
            }
        }

        // Check held key
        if (heldItem.getItem() instanceof KeyItem && heldItem.has(DataComponents.LOCK)) {
            String heldKeyCode = heldItem.get(DataComponents.LOCK).key();
            if (heldKeyCode.equals(containerKeyCode)) {
                return true;
            }
        }

        // Check inventory for matching key
        return hasMatchingKeyInInventory(player, containerKeyCode);
    }

    private static boolean canAddLock(BaseContainerBlockEntity containerBlockEntity) {
        DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();
        return (!containerBlockEntity.components().has(lockType)) ||
                (containerBlockEntity.components().get(lockType) == LockType.NONE);
    }

    private static void addLock(BaseContainerBlockEntity containerBlockEntity, LockType lockType,
                                Player player, Level level, BlockPos pos, InteractionHand hand) {
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
        DataComponentMap lockChanges = DataComponentMap.builder()
                .set(ModDataComponents.LOCK_TYPE.get(), lockType)
                .set(ModDataComponents.OWNER_UUID.get(), player.getUUID())
                .build();
        applyChangesToBlock(containerBlockEntity, lockChanges);
        player.swing(hand);
    }

    private static void addPlayerLock(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                      Player player, Level level, BlockPos pos, InteractionHand hand) {
        String keyCode = heldItem.get(DataComponents.LOCK).key();
        DataComponentMap lockChanges = DataComponentMap.builder()
                .set(DataComponents.LOCK, new LockCode(keyCode))
                .set(ModDataComponents.OWNER_UUID.get(), player.getUUID())
                .build();
        applyChangesToBlock(containerBlockEntity, lockChanges);

        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        MutableComponent blockName = level.getBlockState(pos).getBlock().getName();
        player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", blockName).append(keyCode), true);
        player.swing(hand, true);
    }

    // ========================================
    // KEY CHECKING UTILITIES
    // ========================================

    private static boolean playerHasCorrectKey(Player player, BaseContainerBlockEntity containerBlockEntity) {
        ItemStack heldItem = player.getMainHandItem();

        // Check special lock types
        DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();
        if (containerBlockEntity.components().has(lockType)) {
            LockType currentLockType = containerBlockEntity.components().get(lockType);

            if (currentLockType == LockType.GOLDEN) {
                return heldItem.is(ModItems.GOLDEN_KEY.get()) ||
                        hasItemInInventory(player, ModItems.GOLDEN_KEY.get());
            } else if (currentLockType == LockType.TRIAL) {
                return heldItem.is(Items.TRIAL_KEY) ||
                        hasItemInInventory(player, Items.TRIAL_KEY);
            }
        }

        // Check regular locks
        if (containerBlockEntity.components().has(DataComponents.LOCK)) {
            String containerKeyCode = containerBlockEntity.components().get(DataComponents.LOCK).key();

            // Check held key
            if (heldItem.getItem() instanceof KeyItem && heldItem.has(DataComponents.LOCK)) {
                if (heldItem.get(DataComponents.LOCK).key().equals(containerKeyCode)) {
                    return true;
                }
            }

            // Check accessory slot key
            Optional<ItemStack> curiosKey = Services.PLATFORM.findEquippedKey(player);
            if (curiosKey.isPresent() && curiosKey.get().has(DataComponents.LOCK)) {
                if (curiosKey.get().get(DataComponents.LOCK).key().equals(containerKeyCode)) {
                    return true;
                }
            }

            // Check inventory for matching key
            return hasMatchingKeyInInventory(player, containerKeyCode);
        }

        return false;
    }

    private static boolean hasItemInInventory(Player player, Item item) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(item));
    }

    private static boolean hasMatchingKeyInInventory(Player player, String keyCode) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() &&
                    stack.getItem() instanceof KeyItem &&
                    stack.has(DataComponents.LOCK) &&
                    stack.get(DataComponents.LOCK).key().equals(keyCode)) {
                return true;
            }
        }
        return false;
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Applies {@code lockChanges} (only the lock components being changed) to the container, and to the other
     * half of a double chest. Each half keeps its own other components, such as its dye color.
     */
    private static void applyChangesToBlock(BaseContainerBlockEntity containerBlockEntity, DataComponentMap lockChanges) {
        mergeComponents(containerBlockEntity, lockChanges);

        Level level = containerBlockEntity.getLevel();
        BlockState blockState = containerBlockEntity.getBlockState();

        // Handle double chests
        if (blockState.getBlock() instanceof ChestBlock && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction facing = ChestBlock.getConnectedDirection(blockState);
            BlockPos otherPos = containerBlockEntity.getBlockPos().relative(facing);
            BlockState otherState = level.getBlockState(otherPos);

            if (isValidChestPartner(blockState, otherState)) {
                BlockEntity otherBlockEntity = level.getBlockEntity(otherPos);
                if (otherBlockEntity instanceof BaseContainerBlockEntity otherContainer) {
                    mergeComponents(otherContainer, lockChanges);
                }
            }
        }
    }

    private static void mergeComponents(BaseContainerBlockEntity container, DataComponentMap changes) {
        container.setComponents(DataComponentMap.builder()
                .addAll(container.components())
                .addAll(changes)
                .build());
        container.setChanged();
    }

    private static boolean isValidChestPartner(BlockState blockState, BlockState otherState) {
        return otherState.getBlock() == blockState.getBlock() &&
                otherState.getValue(ChestBlock.TYPE) != ChestType.SINGLE &&
                otherState.getValue(ChestBlock.FACING) == blockState.getValue(ChestBlock.FACING);
    }

    private static void failedToOpen(Player player, Level level, BlockPos pos) {
        MutableComponent blockName = level.getBlockState(pos).getBlock().getName();
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", blockName).withStyle(ChatFormatting.RED), true);
    }
}
