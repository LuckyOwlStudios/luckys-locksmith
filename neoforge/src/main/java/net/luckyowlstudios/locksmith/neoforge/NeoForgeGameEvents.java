package net.luckyowlstudios.locksmith.neoforge;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.loot.LootChestLocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Forwards NeoForge game events to the shared lock logic in {@link LockEvents}.
 */
@EventBusSubscriber(modid = Locksmith.MOD_ID)
public class NeoForgeGameEvents {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (LockEvents.isKeyDuplication(event.getLeft(), event.getRight())) {
            event.setOutput(event.getLeft().copy());
            event.setCost(1);
            event.setMaterialCost(1);
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        LockEvents.onKeyDuplicated(event.getEntity(), event.getLeft(), event.getRight());
    }

    @SubscribeEvent
    public static void onTooltipRender(ItemTooltipEvent event) {
        LockEvents.appendTooltip(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!LockEvents.canBreakBlock(event.getPlayer(), (Level) event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        LockEvents.protectLockedBlocksFromExplosion(event.getLevel(), event.getAffectedBlocks());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        switch (LockEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getPos())) {
            case PASS -> {
            }
            case CANCEL -> event.setCanceled(true);
            case CANCEL_FAIL -> {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getChunk() instanceof LevelChunk chunk) {
            LootChestLocks.onChunkLoad(chunk, LocksmithConfig.FORTRESS_CHEST_LOCK_CHANCE.getAsDouble());
        }
    }
}
