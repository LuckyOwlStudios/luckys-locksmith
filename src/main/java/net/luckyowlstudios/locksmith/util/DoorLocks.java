package net.luckyowlstudios.locksmith.util;

import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import top.theillusivec4.curios.api.CuriosApi;

public final class DoorLocks {
    private DoorLocks() {}

    public static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    public static DoorLockData.Entry get(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) return null;
        return DoorLockData.get(server).get(lowerPos(pos, state));
    }

    public static boolean matches(ItemStack stack, String code) {
        LockCode key = stack.get(DataComponents.LOCK);
        return stack.getItem() instanceof KeyItem && key != null && key.key().equals(code);
    }

    public static boolean hasKey(Player player, DoorLockData.Entry lock) {
        if (lock.type() != LockType.LOCK) {
            return player.getInventory().hasAnyMatching(stack -> specialKey(stack, lock.type()));
        }
        if (player.getInventory().hasAnyMatching(stack -> matches(stack, lock.keyCode()))) return true;
        return CuriosApi.getCuriosInventory(player).map(inventory ->
                inventory.findFirstCurio(stack -> matches(stack, lock.keyCode())).isPresent()).orElse(false);
    }

    public static boolean specialKey(ItemStack stack, LockType type) {
        return type == LockType.GOLDEN && stack.is(ModItems.GOLDEN_KEY)
                || type == LockType.TRIAL && stack.is(Items.TRIAL_KEY);
    }

    /** Returns true when this interaction is handled, including rejected attempts. Server only. */
    public static boolean interact(ServerLevel level, BlockPos clickedPos, Player player, InteractionHand hand) {
        BlockState clicked = level.getBlockState(clickedPos);
        if (!(clicked.getBlock() instanceof DoorBlock door)) return false;
        BlockPos pos = lowerPos(clickedPos, clicked);
        BlockState state = level.getBlockState(pos);
        if (!state.is(door) || !level.getBlockState(pos.above()).is(door)) return false;
        DoorLockData data = DoorLockData.get(level);
        DoorLockData.Entry lock = data.get(pos);
        ItemStack held = player.getItemInHand(hand);

        if (lock != null) {
            if (lock.type() == LockType.LOCK && hasKey(player, lock)) {
                door.setOpen(player, level, state, pos, !state.getValue(DoorBlock.OPEN));
            } else if (specialKey(held, lock.type())) {
                data.remove(pos);
                // As with chest special locks, unlocking consumes the key and removes the lock.
                held.shrink(1);
                door.setOpen(player, level, state, pos, true);
                level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1, 1.5F);
            } else {
                failed(player, level, pos);
            }
            correctPrediction(player, level, pos);
            return true;
        }

        LockType type;
        String code = "";
        if (held.is(ModItems.GOLDEN_LOCK)) type = LockType.GOLDEN;
        else if (held.is(ModItems.TRIAL_LOCK)) type = LockType.TRIAL;
        else if (held.getItem() instanceof KeyItem) {
            LockCode key = held.get(DataComponents.LOCK);
            if (key == null || key.key().isEmpty()) return true;
            type = LockType.LOCK;
            code = key.key();
        } else return false;

        data.put(pos, new DoorLockData.Entry(type, code, player.getUUID()));
        door.setOpen(player, level, state, pos, false);
        // Clear old power on both halves so redstone resumes normally after removing a special lock.
        for (BlockPos half : new BlockPos[]{pos, pos.above()}) {
            level.setBlock(half, level.getBlockState(half).setValue(DoorBlock.POWERED, false), 2);
        }
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1, 1);
        player.displayClientMessage(Component.translatable("message.locksmith.block_add_lock", state.getBlock().getName()).append(code), true);
        correctPrediction(player, level, pos);
        return true;
    }

    public static void failed(Player player, Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, player.getSoundSource(), 1, 1);
        player.displayClientMessage(Component.translatable("message.locksmith.block_locked", level.getBlockState(pos).getBlock().getName()), true);
    }

    private static void correctPrediction(Player player, ServerLevel level, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos.above()));
        }
    }
}
