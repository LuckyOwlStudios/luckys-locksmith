package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    // Ran on the server and client when a player right-clicks a block.
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
                    containerBlockEntity.setComponents(DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.NONE).build());
                    containerBlockEntity.setChanged();
                    player.swing(hand);
                    heldItem.shrink(1);
                    event.setCanceled(true);
                } else {
                    failedToOpen(event);
                }
            }
            if (containerBlockEntity.components().get(lockType) == LockType.TRIAL) {
                if (heldItem.is(Items.TRIAL_KEY)) {
                    level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
                    BlockState chainState = Blocks.CHAIN.defaultBlockState();
                    level.playSound(null, pos, chainState.getBlock().getSoundType(chainState, level, pos, null).getBreakSound(), player.getSoundSource(), 1.0F, 1.0F);
                    containerBlockEntity.setComponents(DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.NONE).build());
                    containerBlockEntity.setChanged();
                    player.swing(hand);
                    heldItem.shrink(1);
                    event.setCanceled(true);
                } else {
                    failedToOpen(event);
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
                    failedToOpen(event);
                }
            }
        } else if ((containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get()) && containerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get()) == LockType.NONE) || !containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
            if (heldItem.is(ModItems.GOLDEN_LOCK)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
                containerBlockEntity.setComponents(DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.GOLDEN).build());
                containerBlockEntity.setChanged();
                player.swing(hand);
                event.setCanceled(true);
            } else if (heldItem.is(ModItems.TRIAL_LOCK)) {
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.5F);
                containerBlockEntity.setComponents(DataComponentMap.builder().addAll(containerBlockEntity.components()).set(ModDataComponents.LOCK_TYPE, LockType.TRIAL).build());
                containerBlockEntity.setChanged();
                player.swing(hand);
                event.setCanceled(true);
            }
        }

        // 2. Handle setting new lock if player is holding a key
        if (isHeldKey) {
            String keyCode = heldItem.get(DataComponents.LOCK).key();
            containerBlockEntity.setComponents(DataComponentMap.builder().addAll(containerBlockEntity.components()).set(DataComponents.LOCK, new LockCode(keyCode)).build());
            containerBlockEntity.setChanged();
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", getBlockName).append(keyCode), true);
            player.swing(event.getHand(), true);
            event.setCanceled(true);
        }
    }

    private static void failedToOpen(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getEntity().level();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        MutableComponent getBlockName = level.getBlockState(pos).getBlock().getName();
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", getBlockName), true);
        player.swing(event.getHand(), true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }
}
