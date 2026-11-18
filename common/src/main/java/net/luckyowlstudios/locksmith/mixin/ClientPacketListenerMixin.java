package net.luckyowlstudios.locksmith.mixin;

import net.luckyowlstudios.locksmith.door.DoorLocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the client gets a chunk, it forgets the door locks it had for it; the server sends the
 * current ones right after the chunk. Without this a lock removed while the player was away would
 * still look locked when they came back.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    // TAIL only runs on the client thread; the first call from the network thread returns early
    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void locksmith$forgetDoorLocks(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            DoorLocks.clearClientChunk(Minecraft.getInstance().level, packet.getX(), packet.getZ());
        }
    }
}
