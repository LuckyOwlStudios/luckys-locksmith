package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Locksmith.MOD_ID)
public class GameEvents {

    // ========================================
    // ANVIL EVENTS - Key duplication system
    // ========================================

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.is(ModItems.KEY.get()) && right.is(Items.IRON_INGOT)) {
            event.setOutput(left.copy());
            event.setCost(1);
            event.setMaterialCost(1);
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack left = event.getLeft();
        Player player = event.getEntity();

        if (left.is(ModItems.KEY.get()) && event.getRight().is(Items.IRON_INGOT)) {
            if (!player.getInventory().add(left.copy())) {
                player.drop(left.copy(), false);
            }
        }
    }

    // ========================================
    // TOOLTIP EVENTS - Enhanced key tooltips
    // ========================================

    @SubscribeEvent
    public static void onTooltipRender(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (itemStack.is(Items.TRIAL_KEY)) {
            tooltip.add(Component.translatable("tooltip.locksmith.trial_key").withStyle(ChatFormatting.GRAY));
        } else if (itemStack.is(Items.OMINOUS_TRIAL_KEY)) {
            tooltip.add(Component.translatable("tooltip.locksmith.ominous_trial_key").withStyle(ChatFormatting.GRAY));
        }
    }

    // ========================================
    // BLOCK INTERACTION EVENTS
    // ========================================

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity) {
            if (LockHandler.containerHasLock(containerBlockEntity) && !player.isCreative()) {
                // Owner can always break their own locked chest
                UUID owner = containerBlockEntity.components().get(ModDataComponents.OWNER_UUID.get());
                if (owner != null && owner.equals(player.getUUID())) {
                    return; // allow break
                }

                if (!playerHasCorrectKey(player, containerBlockEntity)) {
                    failedToOpen(player, (Level) level, pos);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        List<BlockPos> affectedBlocks = event.getAffectedBlocks();

        // Remove locked blocks from explosion list (only mod locks, not player locks)
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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos pos = event.getPos();

        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity)) return;

        ItemStack heldItem = player.getMainHandItem();
        InteractionHand hand = event.getHand();

        // Handle special lock types (Golden/Trial)
        if (handleSpecialLocks(containerBlockEntity, heldItem, player, level, pos, hand, event)) {
            return;
        }

        // Handle regular locks and key operations
        handleRegularLocks(containerBlockEntity, heldItem, player, level, pos, hand, event);
    }

    // ========================================
    // LOCK HANDLING HELPER METHODS
    // ========================================

    private static boolean handleSpecialLocks(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                              Player player, Level level, BlockPos pos, InteractionHand hand,
                                              PlayerInteractEvent.RightClickBlock event) {
        DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();

        if (!containerBlockEntity.components().has(lockType)) {
            return false;
        }

        LockType currentLockType = containerBlockEntity.components().get(lockType);
        boolean hasCorrectKey = false;

        // Check for correct key based on lock type
        if (currentLockType == LockType.GOLDEN && heldItem.is(ModItems.GOLDEN_KEY)) {
            hasCorrectKey = true;
        } else if (currentLockType == LockType.TRIAL && heldItem.is(Items.TRIAL_KEY)) {
            hasCorrectKey = true;
        }

        if (hasCorrectKey) {
            // Unlock the container
            unlockContainer(containerBlockEntity, player, level, pos, hand, heldItem);
            event.setCanceled(true);
            return true;
        } else if (currentLockType != LockType.NONE) {
            // Wrong key or no key
            failedToOpen(player, level, pos);
            player.swing(hand, true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return true;
        }

        return false;
    }

    private static void handleRegularLocks(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                           Player player, Level level, BlockPos pos, InteractionHand hand,
                                           PlayerInteractEvent.RightClickBlock event) {
        boolean isHeldKey = heldItem.getItem() instanceof KeyItem;
        Optional<ItemStack> curiosKey = getCuriosKey(player);

        // Handle existing locks
        if (containerBlockEntity.components().has(DataComponents.LOCK)) {
            if (canUnlockWithKey(containerBlockEntity, heldItem, curiosKey, player)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                return;
            } else if (!player.isCrouching() || !(heldItem.getItem() instanceof BlockItem)) {
                failedToOpen(player, level, pos);
                player.swing(hand, true);
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }

        // Handle adding new locks
        if (canAddLock(containerBlockEntity)) {
            if (heldItem.is(ModItems.GOLDEN_LOCK)) {
                addLock(containerBlockEntity, LockType.GOLDEN, player, level, pos, hand, event);
            } else if (heldItem.is(ModItems.TRIAL_LOCK)) {
                addLock(containerBlockEntity, LockType.TRIAL, player, level, pos, hand, event);
            } else if (isHeldKey && !containerBlockEntity.components().has(DataComponents.LOCK)) {
                addPlayerLock(containerBlockEntity, heldItem, player, level, pos, hand, event);
            }
        }
    }

    private static void unlockContainer(BaseContainerBlockEntity containerBlockEntity, Player player,
                                        Level level, BlockPos pos, InteractionHand hand, ItemStack heldItem) {
        // Play unlock sounds
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
        BlockState chainState = Blocks.CHAIN.defaultBlockState();
        level.playSound(null, pos, chainState.getBlock().getSoundType(chainState, level, pos, null).getBreakSound(),
                player.getSoundSource(), 1.0F, 1.0F);

        // Remove lock
        DataComponentMap newData = DataComponentMap.builder()
                .addAll(containerBlockEntity.components())
                .set(ModDataComponents.LOCK_TYPE, LockType.NONE)
                .build();
        applyChangesToBlock(containerBlockEntity, newData);

        player.swing(hand);
        heldItem.shrink(1);
    }

    private static boolean canUnlockWithKey(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                            Optional<ItemStack> curiosKey, Player player) {
        String containerKeyCode = containerBlockEntity.components().get(DataComponents.LOCK).key();

        // Check Curios key
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
                                Player player, Level level, BlockPos pos, InteractionHand hand,
                                PlayerInteractEvent.RightClickBlock event) {
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
        DataComponentMap newData = DataComponentMap.builder()
                .addAll(containerBlockEntity.components())
                .set(ModDataComponents.LOCK_TYPE, lockType)
                .set(ModDataComponents.OWNER_UUID, player.getUUID()) // <-- add this
                .build();
        applyChangesToBlock(containerBlockEntity, newData);
        player.swing(hand);
        event.setCanceled(true);
    }

    private static void addPlayerLock(BaseContainerBlockEntity containerBlockEntity, ItemStack heldItem,
                                      Player player, Level level, BlockPos pos, InteractionHand hand,
                                      PlayerInteractEvent.RightClickBlock event) {
        String keyCode = heldItem.get(DataComponents.LOCK).key();
        DataComponentMap newData = DataComponentMap.builder()
                .addAll(containerBlockEntity.components())
                .set(DataComponents.LOCK, new LockCode(keyCode))
                .set(ModDataComponents.OWNER_UUID, player.getUUID())
                .build();
        applyChangesToBlock(containerBlockEntity, newData);

        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        MutableComponent blockName = level.getBlockState(pos).getBlock().getName();
        player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", blockName).append(keyCode), true);
        player.swing(hand, true);
        event.setCanceled(true);
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
                return heldItem.is(ModItems.GOLDEN_KEY) ||
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

            // Check Curios key
            Optional<ItemStack> curiosKey = getCuriosKey(player);
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

    private static boolean hasItemInInventory(Player player, net.minecraft.world.item.Item item) {
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

    private static Optional<ItemStack> getCuriosKey(Player player) {
        ICuriosItemHandler curiosInventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curiosInventory == null) return Optional.empty();

        return curiosInventory.getStacksHandler("key").map(slotInventory -> {
            IItemHandler handler = slotInventory.getStacks();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack itemStack = handler.getStackInSlot(i);
                if (!itemStack.isEmpty() && itemStack.getItem() == ModItems.KEY.asItem()) {
                    return itemStack;
                }
            }
            return ItemStack.EMPTY;
        }).filter(stack -> !stack.isEmpty());
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    private static void applyChangesToBlock(BaseContainerBlockEntity containerBlockEntity, DataComponentMap newData) {
        containerBlockEntity.setComponents(newData);
        containerBlockEntity.setChanged();

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
                    otherContainer.setComponents(newData);
                    otherContainer.setChanged();
                }
            }
        }
    }

    private static boolean isValidChestPartner(BlockState blockState, BlockState otherState) {
        return otherState.getBlock() == blockState.getBlock() &&
                otherState.getValue(ChestBlock.TYPE) != ChestType.SINGLE &&
                otherState.getValue(ChestBlock.FACING) == blockState.getValue(ChestBlock.FACING);
    }

    private static void failedToOpen(Player player, Level level, BlockPos pos) {
        MutableComponent blockName = level.getBlockState(pos).getBlock().getName();
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", blockName), true);
    }
}