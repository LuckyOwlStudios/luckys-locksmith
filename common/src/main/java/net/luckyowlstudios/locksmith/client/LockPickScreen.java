package net.luckyowlstudios.locksmith.client;

import net.luckyowlstudios.locksmith.event.LockPicking;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.network.LockPickResultPayload;
import net.luckyowlstudios.locksmith.network.OpenLockPickPayload;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The lock picking mini game. Styled like a vanilla container screen: the vanilla gray panel, dark
 * text and inset slot-like track. A lock pick slides back and forth over the track, and clicking
 * (or pressing space) while it is over the green sweet spot sets a tumbler. Set all of them to pick
 * the lock; miss once, or close the screen, and the pick snaps.
 */
public class LockPickScreen extends Screen {

    // Vanilla's own gray panel (the demo screen background), so no new GUI texture is needed
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 166;

    private static final int TRACK_HEIGHT = 18;
    private static final int TRACK_Y = 76;

    // Vanilla inventory colors
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SWEET_SPOT = 0xFF80FF20;
    private static final int SWEET_SPOT_EDGE = 0xFF2E8B00;
    private static final int MARKER = 0xFFFFFFFF;

    private static final float SPEED_UP_PER_PIN = 1.15F;

    private final RandomSource random = RandomSource.create();
    private final LockPicking.Difficulty difficulty;

    private int pinsSet;
    private int zoneStart;
    private float speed;
    private long pinStartMillis;
    private boolean done;
    /** Shows the miss for a moment before the screen closes. */
    private long closeAtMillis;

    public LockPickScreen(OpenLockPickPayload payload) {
        super(Component.translatable("screen.locksmith.lock_pick"));
        this.difficulty = LockPicking.difficulty(payload.ownerLevels());
        this.speed = difficulty.speed();
        nextPin();
    }

    private void nextPin() {
        int width = difficulty.zoneWidth();
        zoneStart = 2 + random.nextInt(LockPicking.TRACK_WIDTH - width - 3);
        pinStartMillis = Util.getMillis();
    }

    /** Where the pick is on the track right now, 0 to {@link LockPicking#TRACK_WIDTH} - 1. */
    private float markerPosition() {
        float phase = ((Util.getMillis() - pinStartMillis) / 1000.0F * speed) % 2.0F;
        float t = phase < 1.0F ? phase : 2.0F - phase;
        return t * (LockPicking.TRACK_WIDTH - 1);
    }

    private boolean markerInZone() {
        float pos = markerPosition();
        return pos >= zoneStart - 0.5F && pos <= zoneStart + difficulty.zoneWidth() - 0.5F;
    }

    private void attempt() {
        if (done) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (markerInZone()) {
            pinsSet++;
            if (pinsSet >= difficulty.pins()) {
                finish(true);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VAULT_INSERT_ITEM, 1.5F));
                onClose();
            } else {
                speed *= SPEED_UP_PER_PIN;
                nextPin();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_PLACE, 1.0F + pinsSet * 0.15F));
            }
        } else {
            finish(false);
            closeAtMillis = Util.getMillis() + 700;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK, 1.0F));
        }
    }

    private void finish(boolean success) {
        done = true;
        Services.PLATFORM.sendToServer(new LockPickResultPayload(success));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (done && closeAtMillis != 0 && Util.getMillis() >= closeAtMillis) {
            onClose();
            return;
        }

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.blit(PANEL, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, title, left + 8, top + 8, TEXT_COLOR, false);
        Component hint = Component.translatable("screen.locksmith.lock_pick.hint");
        graphics.drawString(font, hint, left + 8, top + 22, TEXT_COLOR, false);

        // Tumblers set so far, drawn as slots that fill up green
        int pinsWidth = difficulty.pins() * 20 - 2;
        int pinX = left + (PANEL_WIDTH - pinsWidth) / 2;
        for (int i = 0; i < difficulty.pins(); i++) {
            drawSlot(graphics, pinX + i * 20, top + 34, 18, 16, i < pinsSet ? SWEET_SPOT : SLOT_FILL);
        }

        int trackX = left + (PANEL_WIDTH - LockPicking.TRACK_WIDTH) / 2;
        int trackY = top + TRACK_Y;
        drawSlot(graphics, trackX, trackY, LockPicking.TRACK_WIDTH, TRACK_HEIGHT, SLOT_FILL);

        if (!done) {
            int zoneX = trackX + zoneStart;
            graphics.fill(zoneX, trackY, zoneX + difficulty.zoneWidth(), trackY + TRACK_HEIGHT, SWEET_SPOT);
            if (difficulty.zoneWidth() > 2) {
                graphics.fill(zoneX, trackY, zoneX + 1, trackY + TRACK_HEIGHT, SWEET_SPOT_EDGE);
                graphics.fill(zoneX + difficulty.zoneWidth() - 1, trackY, zoneX + difficulty.zoneWidth(), trackY + TRACK_HEIGHT, SWEET_SPOT_EDGE);
            }
            int markerX = trackX + Math.round(markerPosition());
            graphics.fill(markerX, trackY, markerX + 1, trackY + TRACK_HEIGHT, MARKER);
            // The lock pick itself rides along above the track
            graphics.renderItem(new ItemStack(ModItems.LOCK_PICK.get()), markerX - 8, trackY - 17);
        } else if (closeAtMillis != 0) {
            Component snapped = Component.translatable("message.locksmith.lock_pick_snapped");
            graphics.drawString(font, snapped, left + (PANEL_WIDTH - font.width(snapped)) / 2, trackY + 5, 0xFFAA0000, false);
        }

        Component progress = Component.translatable("screen.locksmith.lock_pick.tumblers", pinsSet, difficulty.pins());
        graphics.drawString(font, progress, left + (PANEL_WIDTH - font.width(progress)) / 2, top + 100, TEXT_COLOR, false);
    }

    /** A vanilla style inset slot: dark top and left edge, light bottom and right edge. */
    private static void drawSlot(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_LIGHT);
        graphics.fill(x - 1, y - 1, x + w, y + h, SLOT_DARK);
        graphics.fill(x, y, x + w, y + h, fill);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            attempt();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            attempt();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Closing the screen early counts as a miss. */
    @Override
    public void removed() {
        if (!done) {
            finish(false);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
