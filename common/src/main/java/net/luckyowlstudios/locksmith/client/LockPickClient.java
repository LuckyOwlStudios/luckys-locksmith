package net.luckyowlstudios.locksmith.client;

import net.luckyowlstudios.locksmith.network.OpenLockPickPayload;
import net.minecraft.client.Minecraft;

/** Client-only entry point, so the loaders' common packet code never loads screen classes on a server. */
public final class LockPickClient {

    private LockPickClient() {
    }

    public static void open(OpenLockPickPayload payload) {
        Minecraft.getInstance().setScreen(new LockPickScreen(payload));
    }
}
