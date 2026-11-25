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
    private static final int SWEET_SPOT_LIGHT = 0xFFC6FF8A;
    private static final int MARKER = 0xFFFFFFFF;
    private static final int MARKER_OUTLINE = 0xFF202020;
    private static final int TRACK_FILL = 0xFF4A4A4A;
    private static final int TRACK_TICK = 0xFF6E6E6E;
    private static final int MISS_FILL = 0xFFAA2A2A;
    private static final int HINT_COLOR = 0xFF707070;
    private static final int CAVITY = 0xFF2B2B2B;
    private static final int SPRING = 0xFF8B8B8B;
    private static final int CURRENT_PIN = 0xFFFFFF55;
    private static final int LIGHT_OFF = 0xFF555555;
    private static final int BRASS_LIGHT = 0xFFF4D26B;
    private static final int BRASS_DARK = 0xFFB8862B;
    private static final int BRASS_OUTLINE = 0xFF5A3E10;
    private static final int STEEL_LIGHT = 0xFFC1C1C1;
    private static final int STEEL_DARK = 0xFF6E6E6E;
    private static final int STEEL_OUTLINE = 0xFF1E1E1E;

    private static final float SPEED_UP_PER_PIN = 1.15F;

    private final RandomSource random = RandomSource.create();
    private final LockPicking.Difficulty difficulty;
    private final int ownerLevels;

    private int pinsSet;
    private int zoneStart;
    private float speed;
    private long pinStartMillis;
    private boolean done;
    /** Shows the miss for a moment before the screen closes. */
    private long closeAtMillis;

    public LockPickScreen(OpenLockPickPayload payload) {
        super(Component.translatable("screen.locksmith.lock_pick"));
        this.ownerLevels = payload.ownerLevels();
        this.difficulty = LockPicking.difficulty(ownerLevels);
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
        int center = left + PANEL_WIDTH / 2;
        graphics.blit(PANEL, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, title, center - font.width(title) / 2, top + 9, TEXT_COLOR, false);

        // The tumblers: a spring pushes each pin down until it is set
        int pinsWidth = difficulty.pins() * PIN_SPACING - (PIN_SPACING - PIN_WIDTH);
        int pinX = center - pinsWidth / 2;
        for (int i = 0; i < difficulty.pins(); i++) {
            drawPin(graphics, pinX + i * PIN_SPACING, top + PIN_Y, i < pinsSet, i == pinsSet && !done);
        }

        // The cylinder the pick slides along
        int trackX = center - LockPicking.TRACK_WIDTH / 2;
        int trackY = top + TRACK_Y;
        drawSlot(graphics, trackX, trackY, LockPicking.TRACK_WIDTH, TRACK_HEIGHT, TRACK_FILL);
        for (int x = 8; x < LockPicking.TRACK_WIDTH; x += 8) {
            graphics.fill(trackX + x, trackY + TRACK_HEIGHT - 4, trackX + x + 1, trackY + TRACK_HEIGHT, TRACK_TICK);
        }

        if (!done) {
            int zoneX = trackX + zoneStart;
            int zoneEnd = zoneX + difficulty.zoneWidth();
            graphics.fill(zoneX, trackY, zoneEnd, trackY + TRACK_HEIGHT, SWEET_SPOT);
            graphics.fill(zoneX, trackY, zoneEnd, trackY + 1, SWEET_SPOT_LIGHT);
            if (difficulty.zoneWidth() > 3) {
                graphics.fill(zoneX, trackY, zoneX + 1, trackY + TRACK_HEIGHT, SWEET_SPOT_EDGE);
                graphics.fill(zoneEnd - 1, trackY, zoneEnd, trackY + TRACK_HEIGHT, SWEET_SPOT_EDGE);
            }
            int markerX = trackX + Math.round(markerPosition());
            graphics.fill(markerX - 1, trackY, markerX + 2, trackY + TRACK_HEIGHT, MARKER_OUTLINE);
            graphics.fill(markerX, trackY, markerX + 1, trackY + TRACK_HEIGHT, MARKER);
            // The lock pick itself rides along above the cylinder
            graphics.renderItem(new ItemStack(ModItems.LOCK_PICK.get()), markerX - 8, trackY - 17);
        } else if (closeAtMillis != 0) {
            graphics.fill(trackX, trackY, trackX + LockPicking.TRACK_WIDTH, trackY + TRACK_HEIGHT, MISS_FILL);
            Component snapped = Component.translatable("message.locksmith.lock_pick_snapped");
            graphics.drawString(font, snapped, center - font.width(snapped) / 2, trackY + 5, 0xFFFFFFFF, true);
        }

        Component hint = Component.translatable("screen.locksmith.lock_pick.hint");
        graphics.drawString(font, hint, center - font.width(hint) / 2, top + 113, TEXT_COLOR, false);
        Component owner = Component.translatable("screen.locksmith.lock_pick.owner", ownerLevels);
        graphics.drawString(font, owner, center - font.width(owner) / 2, top + 135, HINT_COLOR, false);
    }

    /** One tumbler: a dark cavity with a spring and a brass pin that rises when set. */
    private static void drawPin(GuiGraphics graphics, int x, int y, boolean set, boolean current) {
        if (current) {
            graphics.fill(x - 2, y - 2, x + PIN_WIDTH + 2, y + PIN_HEIGHT + 2, CURRENT_PIN);
        }
        drawSlot(graphics, x, y, PIN_WIDTH, PIN_HEIGHT, CAVITY);

        int pinTop = y + (set ? 3 : 17);
        // Spring, squeezed when the pin is up
        for (int yy = y + 1, i = 0; yy < pinTop - 1; yy += 2, i++) {
            int sx = x + 7 + (i % 2) * 2;
            graphics.fill(sx, yy, sx + 4, yy + 1, SPRING);
        }
        int pinLeft = x + 5;
        int pinRight = x + PIN_WIDTH - 5;
        graphics.fill(pinLeft - 1, pinTop - 1, pinRight + 1, pinTop + 15, set ? BRASS_OUTLINE : STEEL_OUTLINE);
        graphics.fillGradient(pinLeft, pinTop, pinRight, pinTop + 14, set ? BRASS_LIGHT : STEEL_LIGHT, set ? BRASS_DARK : STEEL_DARK);
        // A small light under the tumbler shows it is set
        graphics.fill(x + PIN_WIDTH / 2 - 2, y + PIN_HEIGHT + 4, x + PIN_WIDTH / 2 + 2, y + PIN_HEIGHT + 7, set ? SWEET_SPOT : LIGHT_OFF);
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
