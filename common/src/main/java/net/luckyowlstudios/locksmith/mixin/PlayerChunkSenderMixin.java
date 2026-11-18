package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.door.DoorLocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends the door locks in a chunk right after the chunk itself, on both loaders.
 */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @Inject(method = "sendChunk", at = @At("TAIL"))
    private static void locksmith$sendDoorLocks(ServerGamePacketListenerImpl packetListener, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        DoorLocks.onChunkSent(packetListener.player, level, chunk.getPos());
    }
}
