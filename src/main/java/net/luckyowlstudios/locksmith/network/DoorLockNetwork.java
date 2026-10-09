package net.luckyowlstudios.locksmith.network;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.client.DoorLockRenderer;
import net.luckyowlstudios.locksmith.util.DoorLockData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Locksmith.MOD_ID)
public final class DoorLockNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(DoorLocksPayload.TYPE, DoorLocksPayload.STREAM_CODEC,
                (payload, context) -> DoorLockRenderer.accept(payload));
    }

    public static DoorLocksPayload snapshot(ServerLevel level, ChunkPos chunk) {
        return new DoorLocksPayload(level.dimension().location(), chunk.toLong(), DoorLockData.get(level).visuals(chunk));
    }

    public static void changed(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, snapshot(level, chunk));
    }

    @SubscribeEvent
    public static void sent(ChunkWatchEvent.Sent event) {
        PacketDistributor.sendToPlayer(event.getPlayer(), snapshot(event.getLevel(), event.getPos()));
    }
}
