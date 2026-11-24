package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.LockPickItem;
import net.luckyowlstudios.locksmith.network.LockPickResultPayload;
import net.luckyowlstudios.locksmith.network.OpenLockPickPayload;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The lock picking mini game, server side. The server starts an attempt (which costs one use of the
 * pick), the client plays the game, and the server checks the result against that attempt.
 * <p>
 * The harder part is shared with the client through {@link #difficulty(int)}: the more experience
 * levels the lock's owner has, the more tumblers, the smaller the sweet spot and the faster the pick
 * moves. From level {@value #IMPOSSIBLE_LEVEL} on the sweet spot is only a couple of pixels wide.
 */
public final class LockPicking {

    /** At this many owner levels (or more) the mini game is as hard as it gets. */
    public static final int IMPOSSIBLE_LEVEL = 30;
    /** Width of the track the pick slides over, in GUI pixels. The client draws it this wide. */
    public static final int TRACK_WIDTH = 160;

    private static final int MAX_REACH_SQR = 8 * 8;
    private static final int ATTEMPT_TIMEOUT_TICKS = 20 * 60 * 3;
    /** Even a perfect run takes this long per tumbler, so a result that arrives sooner is not real. */
    private static final int MIN_TICKS_PER_PIN = 8;

    /** One running attempt per player. Only touched from the server thread. */
    private static final Map<UUID, Attempt> ATTEMPTS = new HashMap<>();

    private record Attempt(BlockPos pos, Level level, long startTick, int pins) {
    }

    private LockPicking() {
    }

    /**
     * @param pins        how many tumblers have to be set in a row
     * @param zoneWidth   width of the sweet spot in pixels of the {@link #TRACK_WIDTH} wide track
     * @param speed       how many track lengths per second the pick moves, for the first tumbler
     */
    public record Difficulty(int pins, int zoneWidth, float speed) {
    }

    public static Difficulty difficulty(int ownerLevels) {
        float t = Math.min(Math.max(ownerLevels, 0), IMPOSSIBLE_LEVEL) / (float) IMPOSSIBLE_LEVEL;
        int pins = ownerLevels >= IMPOSSIBLE_LEVEL ? 5 : ownerLevels >= 15 ? 4 : 3;
        int zone = Math.round(40 - 38 * (float) Math.pow(t, 0.7));
        float speed = 0.9F + 1.3F * t;
        return new Difficulty(pins, zone, speed);
    }

    /** Starts an attempt on a player-locked container. Call on the server only. */
    public static void start(ServerPlayer player, BaseContainerBlockEntity container, ItemStack pick) {
        Level level = player.level();
        BlockPos pos = container.getBlockPos();

        UUID owner = container.components().get(ModDataComponents.OWNER_UUID.get());
        ServerPlayer ownerPlayer = owner == null ? null : player.server.getPlayerList().getPlayer(owner);
        // An owner who is offline has no levels to defend the lock with
        int ownerLevels = ownerPlayer == null ? 0 : ownerPlayer.experienceLevel;

        Attempt running = ATTEMPTS.get(player.getUUID());
        if (running != null && level.getGameTime() - running.startTick < ATTEMPT_TIMEOUT_TICKS) {
            return;
        }

        ATTEMPTS.put(player.getUUID(), new Attempt(pos, level, level.getGameTime(), difficulty(ownerLevels).pins));
        // The attempt itself wears the pick
        pick.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, player.getSoundSource(), 1.0F, 1.6F);
        Services.PLATFORM.sendToPlayer(player, new OpenLockPickPayload(pos, ownerLevels));
    }

    /** The client reports how the mini game went. */
    public static void onResult(ServerPlayer player, LockPickResultPayload payload) {
        Attempt attempt = ATTEMPTS.remove(player.getUUID());
        if (attempt == null) return;

        Level level = player.level();
        BlockPos pos = attempt.pos;
        if (level != attempt.level || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_REACH_SQR) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity container)
                || !container.components().has(DataComponents.LOCK)) {
            return;
        }

        long elapsed = level.getGameTime() - attempt.startTick;
        if (payload.success() && elapsed >= (long) attempt.pins * MIN_TICKS_PER_PIN) {
            LockEvents.removePlayerLock(container);
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, player.getSoundSource(), 1.0F, 1.5F);
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, player.getSoundSource(), 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.locksmith.lock_picked").withStyle(ChatFormatting.GREEN), true);
        } else {
            breakPick(player);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK, player.getSoundSource(), 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.locksmith.lock_pick_snapped").withStyle(ChatFormatting.RED), true);
        }
    }

    /** A failed attempt snaps the pick the player is holding. */
    private static void breakPick(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.LOCK_PICK.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    /** True if this lock is one a pick can open: a player lock, not a golden or trial lock. */
    public static boolean isPickable(BaseContainerBlockEntity container) {
        return container.components().has(DataComponents.LOCK);
    }

    public static boolean isPick(ItemStack stack) {
        return stack.getItem() instanceof LockPickItem;
    }
}
