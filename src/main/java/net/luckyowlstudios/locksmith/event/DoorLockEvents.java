package net.luckyowlstudios.locksmith.event;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.util.DoorLockData;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

@EventBusSubscriber(modid = Locksmith.MOD_ID)
public final class DoorLockEvents {
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level
                && DoorLocks.interact(level, event.getPos(), event.getEntity(), event.getHand())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    /** Also protects the support block, which otherwise lets a player drop a locked door. */
    private static BlockPos protectedDoor(Level level, BlockPos pos) {
        if (DoorLocks.get(level, pos) != null) return pos;
        if (DoorLocks.get(level, pos.above()) != null) return pos.above();
        return null;
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos door = protectedDoor(level, event.getPos());
        if (door == null) return;
        DoorLockData.Entry lock = DoorLocks.get(level, door);
        var player = event.getPlayer();
        if (!player.isCreative() && !player.getUUID().equals(lock.owner()) && !DoorLocks.hasKey(player, lock)) {
            event.setCanceled(true);
            DoorLocks.failed(player, level, door);
        }
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> protectedDoor(event.getLevel(), pos) != null);
    }

    @SubscribeEvent
    public static void mobBreak(LivingDestroyBlockEvent event) {
        if (protectedDoor(event.getEntity().level(), event.getPos()) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void piston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve()
                && (resolver.getToPush().stream().anyMatch(pos -> protectedDoor(level, pos) != null)
                || resolver.getToDestroy().stream().anyMatch(pos -> protectedDoor(level, pos) != null))) {
            event.setCanceled(true);
        }
    }
}
