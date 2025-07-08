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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber(modid = Locksmith.MOD_ID)
public class GameEvents {
    // Allows players to duplicate keys in the anvil using iron ingots.
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.is(ModItems.KEY.get()) && right.is(Items.IRON_INGOT)) {
            ItemStack result = left.copy(); // duplicated key

            event.setOutput(result);
            event.setCost(1);          // XP level cost
            event.setMaterialCost(1);  // consume only the iron
        }
    }

    // When a key is crafted, we need to make sure the player gets the original key back
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack left = event.getLeft(); // original key

        if (left.is(ModItems.KEY.get()) && event.getRight().is(Items.IRON_INGOT)) {
            // Give the original back to the player
            Player player = event.getEntity();
            if (!player.getInventory().add(left.copy())) {
                // Drop if inventory full
                player.drop(left.copy(), false);
            }
        }
    }

    // Adding tooltips to existing keys in the game to match the new modded keys!
    @SubscribeEvent
    public static void onTooltipRender(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (itemStack.is(Items.TRIAL_KEY)) tooltip.add(Component.translatable("tooltip.locksmith.trial_key").withStyle(ChatFormatting.GRAY));
        if (itemStack.is(Items.OMINOUS_TRIAL_KEY)) tooltip.add(Component.translatable("tooltip.locksmith.ominous_trial_key").withStyle(ChatFormatting.GRAY));
    }

    // Prevents players from breaking locked blocks, such as chests!
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity) {
            boolean isLocked = LockHandler.containerHasLock(containerBlockEntity);
            if (isLocked && !player.isCreative()) {
                failedToOpen(player, (Level) level, pos);
                event.setCanceled(true); // Prevents the block from being broken
            }
        }
    }

    // Only prevent explosions from destroying locked blocks that are not player locks!
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        List<BlockPos> affectedBlocks = event.getAffectedBlocks();

        // Remove locked blocks from the explosion list
        affectedBlocks.removeIf(pos -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof BaseContainerBlockEntity baseContainerBlockEntity && baseContainerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get()) && baseContainerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get()) != LockType.NONE;
        });
    }

    // Ran on the server and client when a player right-clicks a block.
    // Handles opening locked chests with keys, setting locks, and playing sounds.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {

        Player player = event.getEntity();
        Level level = player.level();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);
        MutableComponent getBlockName = blockState.getBlock().getName();
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity containerBlockEntity)) return;

        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) return;

        ItemStack heldItem = player.getMainHandItem();
        boolean isHeldKey = heldItem.getItem() instanceof KeyItem;

        DataComponentType<LockType> lockType = ModDataComponents.LOCK_TYPE.get();
        if (containerBlockEntity.components().has(lockType)) {
            if (containerBlockEntity.components().get(lockType) == LockType.GOLDEN) {
                if (heldItem.is(ModItems.GOLDEN_KEY)) {
                    level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                    BlockState chainState = Blocks.CHAIN.defaultBlockState();
                    level.playSound(null, pos, chainState.getBlock().getSoundType(chainState, level, pos, null).getBreakSound(), player.getSoundSource(), 1.0F, 1.0F);
                    DataComponentMap newData = DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.NONE).build();
                    applyChangesToBlock(containerBlockEntity, newData);
                    player.swing(hand);
                    heldItem.shrink(1);
                    event.setCanceled(true);
                } else {
                    failedToOpen(player, level, pos);
                    player.swing(event.getHand(), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                }
            }
            if (containerBlockEntity.components().get(lockType) == LockType.TRIAL) {
                if (heldItem.is(Items.TRIAL_KEY)) {
                    level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                    BlockState chainState = Blocks.CHAIN.defaultBlockState();
                    level.playSound(null, pos, chainState.getBlock().getSoundType(chainState, level, pos, null).getBreakSound(), player.getSoundSource(), 1.0F, 1.0F);
                    DataComponentMap newData = DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.NONE).build();
                    applyChangesToBlock(containerBlockEntity, newData);
                    player.swing(hand);
                    heldItem.shrink(1);
                    event.setCanceled(true);
                } else {
                    failedToOpen(player, level, pos);
                    player.swing(event.getHand(), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                }
            }
        }

        AtomicBoolean isKeyInCurios = new AtomicBoolean(false);
        AtomicReference<ItemStack> curiosKey = new AtomicReference<>(ItemStack.EMPTY);
        ICuriosItemHandler curiosInventory = CuriosApi.getCuriosInventory(player).get();
        curiosInventory.getStacksHandler("key").ifPresent(slotInventory -> {
            IItemHandler handler = slotInventory.getStacks();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack itemStack = handler.getStackInSlot(i);
                if (!itemStack.isEmpty() && itemStack.getItem() == ModItems.KEY.asItem()) {
                    isKeyInCurios.set(true);
                    curiosKey.set(itemStack);
                }
            }
        });

        // 1. Handle locked chest: Allow if key matches
        if (containerBlockEntity.components().has(DataComponents.LOCK)) {
            if (isKeyInCurios.get() && curiosKey.get().get(DataComponents.LOCK).key().equals(containerBlockEntity.components().get(DataComponents.LOCK).key())) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                return;
            } else if (isHeldKey && heldItem.get(DataComponents.LOCK).key().equals(containerBlockEntity.components().get(DataComponents.LOCK).key())) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                return;
            } else {
                if (!player.isCrouching() || !(heldItem.getItem() instanceof BlockItem)) {
                    failedToOpen(player, level, pos);
                    player.swing(event.getHand(), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    event.setCanceled(true);
                }
            }
        } else if ((containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get()) && containerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get()) == LockType.NONE) || !containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
            if (heldItem.is(ModItems.GOLDEN_LOCK)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
                DataComponentMap newData = DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.GOLDEN).build();
                applyChangesToBlock(containerBlockEntity, newData);
                player.swing(hand);
                event.setCanceled(true);
            } else if (heldItem.is(ModItems.TRIAL_LOCK)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
                DataComponentMap newData = DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.TRIAL).build();
                applyChangesToBlock(containerBlockEntity, newData);
                player.swing(hand);
                event.setCanceled(true);
            }
        }

        // 2. Handle setting new lock if player is holding a key
        if (isHeldKey) {
            String keyCode = heldItem.get(DataComponents.LOCK).key();
            DataComponentMap newData = DataComponentMap.builder().addAll(containerBlockEntity.components()).set(DataComponents.LOCK, new LockCode(keyCode)).build();
            applyChangesToBlock(containerBlockEntity, newData);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", getBlockName).append(keyCode), true);
            player.swing(event.getHand(), true);
            event.setCanceled(true);
        }
    }

    private static void applyChangesToBlock(BaseContainerBlockEntity containerBlockEntity, DataComponentMap newData) {
        containerBlockEntity.setComponents(newData);
        containerBlockEntity.setChanged();

        Level level = containerBlockEntity.getLevel();
        BlockState blockState = containerBlockEntity.getBlockState();

        if (!(blockState.getBlock() instanceof ChestBlock)) return;
        if (blockState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return;

        Direction facing = ChestBlock.getConnectedDirection(blockState);
        BlockPos otherPos = containerBlockEntity.getBlockPos().relative(facing);
        BlockState otherState = level.getBlockState(otherPos);

        // Check if it's the same chest block type
        if (otherState.getBlock() != blockState.getBlock()) return;

        // Check if it's a double chest partner (must be opposite type and facing matches)
        if (otherState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return;
        if (otherState.getValue(ChestBlock.FACING) != blockState.getValue(ChestBlock.FACING)) return;

        BlockEntity otherBlockEntity = level.getBlockEntity(otherPos);
        if (otherBlockEntity instanceof BaseContainerBlockEntity otherContainer) {
            otherContainer.setComponents(newData);
            otherContainer.setChanged();
        }
    }

    private static void failedToOpen(Player player, Level level, BlockPos pos) {
        MutableComponent getBlockName = level.getBlockState(pos).getBlock().getName();
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", getBlockName), true);
    }
}
