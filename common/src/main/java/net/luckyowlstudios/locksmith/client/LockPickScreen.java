package net.luckyowlstudios.locksmith.client;

import net.luckyowlstudios.locksmith.event.LockPicking;
import net.luckyowlstudios.locksmith.network.LockPickResultPayload;
import net.luckyowlstudios.locksmith.network.OpenLockPickPayload;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

/**
 * The lock picking mini game, drawn as a cutaway of a pin tumbler lock. Every unset pin bobs up and
 * down against its spring. Steer the pick under a pin with A and D (or the arrow keys) and press
 * space or click while that pin sits on the green shear line to set it. Set every pin to pick the
 * lock; miss once, or close the screen, and the pick snaps.
 * <p>
 * Everything is drawn with plain fills in a vanilla-like gray palette, so it needs no GUI textures.
 */
public class LockPickScreen extends Screen {

    // Geometry, in GUI pixels
    private static final int PITCH = 28;
    private static final int CHANNEL_WIDTH = 16;
    private static final int CHANNEL_HEIGHT = 66;
    private static final int PIN_LENGTH = 20;
    private static final int PIN_TRAVEL = 44;
    private static final int PLUG_HEIGHT = 24;
    private static final int BODY_HEIGHT = 8 + CHANNEL_HEIGHT + 4 + PLUG_HEIGHT + 6;

    /** How far up its travel a pin is when it sits on the shear line, from 0 (bottom) to 1 (top). */
    private static final float SHEAR = 0.62F;
    private static final float SPEED_UP_PER_PIN = 1.1F;

    // Palette
    private static final int OUTLINE = 0xFF1B1B1B;
    private static final int BODY_LIGHT = 0xFFD2D2D2;
    private static final int BODY_DARK = 0xFF8C8C8C;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int CHANNEL = 0xFF171C1C;
    private static final int CHANNEL_EDGE = 0xFF3A4242;
    private static final int SPRING = 0xFFB4B4B4;
    private static final int SPRING_SHADE = 0xFF6A6A6A;
    private static final int STEEL_LIGHT = 0xFFE3E3E3;
    private static final int STEEL_DARK = 0xFF7C7C7C;
    private static final int BRASS_LIGHT = 0xFFF8DC7A;
    private static final int BRASS_DARK = 0xFFB07A22;
    private static final int GREEN = 0xFF80FF20;
    private static final int GREEN_BAND = 0x6680FF20;
    private static final int SELECTED_BAND = 0x33FFFFFF;
    private static final int RED = 0xFFD83A3A;
    private static final int PLUG = 0xFF222828;
    private static final int PICK_LIGHT = 0xFFE8E8E8;
    private static final int PICK_DARK = 0xFF8A8A8A;
    private static final int LIGHT_OFF = 0xFF4A4A4A;
    private static final int SUBTEXT = 0xFFAAAAAA;

    private final RandomSource random = RandomSource.create();
    private final LockPicking.Difficulty difficulty;
    private final int ownerLevels;

    private final boolean[] set;
    private final float[] phase;
    private final float[] speed;
    private final long startMillis = Util.getMillis();

    private int selected;
    private float pickX;
    private int pinsSet;
    private int missedPin = -1;
    private boolean done;
    /** When the screen closes by itself after the result is shown, or 0 while it is still playing. */
    private long closeAtMillis;

    public LockPickScreen(OpenLockPickPayload payload) {
        super(Component.translatable("screen.locksmith.lock_pick"));
        this.ownerLevels = payload.ownerLevels();
        this.difficulty = LockPicking.difficulty(ownerLevels);
        int pins = difficulty.pins();
        this.set = new boolean[pins];
        this.phase = new float[pins];
        this.speed = new float[pins];
        for (int i = 0; i < pins; i++) {
            phase[i] = random.nextFloat() * 2.0F;
            speed[i] = difficulty.speed() * (0.85F + random.nextFloat() * 0.3F);
        }
        this.selected = pins / 2;
        this.pickX = Float.NaN;
    }

    /** How far up its travel this pin is right now, from 0 (resting) to 1 (against the top). */
    private float pinHeight(int pin) {
        if (set[pin]) {
            return SHEAR;
        }
        float phaseNow = ((Util.getMillis() - startMillis) / 1000.0F * speed[pin] * (1.0F + pinsSet * (SPEED_UP_PER_PIN - 1.0F)) + phase[pin]) % 2.0F;
        return phaseNow < 1.0F ? phaseNow : 2.0F - phaseNow;
    }

    private boolean onShearLine(int pin) {
        float halfZone = difficulty.zoneWidth() / (float) LockPicking.TRACK_WIDTH / 2.0F;
        return Math.abs(pinHeight(pin) - SHEAR) <= halfZone;
    }

    private void move(int by) {
        if (done) return;
        int next = Math.max(0, Math.min(set.length - 1, selected + by));
        if (next != selected) {
            selected = next;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.4F));
        }
    }

    private void attempt() {
        if (done || set[selected]) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (onShearLine(selected)) {
            set[selected] = true;
            pinsSet++;
            if (pinsSet >= set.length) {
                finish(true);
                closeAtMillis = Util.getMillis() + 600;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VAULT_INSERT_ITEM, 1.5F));
            } else {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_PLACE, 1.0F + pinsSet * 0.15F));
                // Move on to the next pin that is still down
                for (int i = 1; i < set.length; i++) {
                    int candidate = (selected + i) % set.length;
                    if (!set[candidate]) {
                        selected = candidate;
                        break;
                    }
                }
            }
        } else {
            missedPin = selected;
            finish(false);
            closeAtMillis = Util.getMillis() + 800;
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

        if (done && Util.getMillis() >= closeAtMillis) {
            onClose();
            return;
        }

        int pins = set.length;
        int bodyWidth = pins * PITCH + 12;
        int left = (width - bodyWidth) / 2;
        int top = (height - BODY_HEIGHT) / 2 - 10;
        int channelTop = top + 8;
        int channelBottom = channelTop + CHANNEL_HEIGHT;
        int plugTop = channelBottom + 4;

        graphics.drawString(font, title, left, top - 12, 0xFFFFFFFF, true);

        drawBody(graphics, left, top, bodyWidth);

        // The shear line, where the pins have to stop
        int shearY = channelBottom - Math.round(SHEAR * PIN_TRAVEL) - PIN_LENGTH;
        int zoneHeight = Math.max(2, Math.round(difficulty.zoneWidth() / (float) LockPicking.TRACK_WIDTH * PIN_TRAVEL));
        for (int i = 0; i < pins; i++) {
            int cx = left + 6 + i * PITCH + (PITCH - CHANNEL_WIDTH) / 2;
            drawChannel(graphics, i, cx, channelTop, channelBottom, shearY, zoneHeight);
        }

        // The plug the pick slides along
        graphics.fill(left + 4, plugTop - 1, left + bodyWidth - 4, plugTop + PLUG_HEIGHT + 1, OUTLINE);
        graphics.fill(left + 5, plugTop, left + bodyWidth - 5, plugTop + PLUG_HEIGHT, PLUG);
        graphics.fill(left + 5, plugTop, left + bodyWidth - 5, plugTop + 1, CHANNEL_EDGE);

        drawPick(graphics, left, plugTop, partialTick);

        // Pins that are set show a green light on the plug
        for (int i = 0; i < pins; i++) {
            int lx = left + 6 + i * PITCH + PITCH / 2;
            graphics.fill(lx - 2, plugTop + PLUG_HEIGHT - 5, lx + 2, plugTop + PLUG_HEIGHT - 2, set[i] ? GREEN : LIGHT_OFF);
        }

        int textY = top + BODY_HEIGHT + 12;
        if (done && missedPin >= 0) {
            Component snapped = Component.translatable("message.locksmith.lock_pick_snapped");
            graphics.drawString(font, snapped, (width - font.width(snapped)) / 2, textY, RED, true);
        } else if (done) {
            Component picked = Component.translatable("message.locksmith.lock_picked");
            graphics.drawString(font, picked, (width - font.width(picked)) / 2, textY, GREEN, true);
        } else {
            Component hint = Component.translatable("screen.locksmith.lock_pick.hint");
            graphics.drawString(font, hint, (width - font.width(hint)) / 2, textY, 0xFFFFFFFF, true);
        }
        Component owner = Component.translatable("screen.locksmith.lock_pick.owner", ownerLevels);
        graphics.drawString(font, owner, (width - font.width(owner)) / 2, textY + 14, SUBTEXT, true);
    }

    /** The steel housing, with a vanilla style bevel. */
    private static void drawBody(GuiGraphics graphics, int left, int top, int bodyWidth) {
        graphics.fill(left - 1, top - 1, left + bodyWidth + 1, top + BODY_HEIGHT + 1, OUTLINE);
        graphics.fillGradient(left, top, left + bodyWidth, top + BODY_HEIGHT, BODY_LIGHT, BODY_DARK);
        graphics.fill(left, top, left + bodyWidth, top + 1, HIGHLIGHT);
        graphics.fill(left, top, left + 1, top + BODY_HEIGHT, HIGHLIGHT);
        // Screws in the corners
        for (int sx : new int[]{left + 3, left + bodyWidth - 5}) {
            for (int sy : new int[]{top + 3, top + BODY_HEIGHT - 5}) {
                graphics.fill(sx, sy, sx + 2, sy + 2, OUTLINE);
            }
        }
    }

    /** One pin channel: dark shaft, shear line band, spring and the pin itself. */
    private void drawChannel(GuiGraphics graphics, int pin, int x, int channelTop, int channelBottom, int shearY, int zoneHeight) {
        boolean current = pin == selected && !done;
        graphics.fill(x - 1, channelTop - 1, x + CHANNEL_WIDTH + 1, channelBottom + 1, current ? 0xFFFFFF55 : OUTLINE);
        graphics.fill(x, channelTop, x + CHANNEL_WIDTH, channelBottom, CHANNEL);
        graphics.fill(x, channelTop, x + 1, channelBottom, CHANNEL_EDGE);

        // The band the pin has to stop in
        graphics.fill(x, shearY - zoneHeight / 2, x + CHANNEL_WIDTH, shearY - zoneHeight / 2 + zoneHeight, GREEN_BAND);
        if (current) {
            graphics.fill(x, channelTop, x + CHANNEL_WIDTH, channelBottom, SELECTED_BAND);
        }

        float height = pinHeight(pin);
        int pinBottom = channelBottom - Math.round(height * PIN_TRAVEL);
        int pinTop = pinBottom - PIN_LENGTH;

        // Spring from the top of the channel down to the pin, tighter the higher the pin is
        int coils = Math.max(2, (pinTop - channelTop) / 3);
        for (int c = 0; c < coils; c++) {
            int cy = channelTop + 1 + c * (pinTop - channelTop - 2) / coils;
            int inset = (c % 2 == 0) ? 3 : 5;
            graphics.fill(x + inset, cy, x + CHANNEL_WIDTH - inset + (c % 2 == 0 ? 0 : 2), cy + 1, c % 2 == 0 ? SPRING : SPRING_SHADE);
        }

        boolean missed = pin == missedPin;
        int light = missed ? RED : set[pin] ? BRASS_LIGHT : STEEL_LIGHT;
        int dark = missed ? 0xFF7A1D1D : set[pin] ? BRASS_DARK : STEEL_DARK;
        graphics.fill(x + 3, pinTop - 1, x + CHANNEL_WIDTH - 3, pinBottom + 1, OUTLINE);
        graphics.fillGradient(x + 4, pinTop, x + CHANNEL_WIDTH - 4, pinBottom, light, dark);
        graphics.fill(x + 4, pinTop, x + 5, pinBottom, 0x88FFFFFF);
    }

    /** The lock pick: a thin shaft that comes in from the left and hooks up under the selected pin. */
    private void drawPick(GuiGraphics graphics, int left, int plugTop, float partialTick) {
        float target = left + 6 + selected * PITCH + PITCH / 2.0F;
        pickX = Float.isNaN(pickX) ? target : pickX + (target - pickX) * Math.min(1.0F, 0.35F * (partialTick + 1.0F));

        int tipX = Math.round(pickX);
        int shaftY = plugTop + PLUG_HEIGHT / 2 + 2;
        int startX = Math.max(0, left - 90);
        graphics.fill(startX, shaftY - 1, tipX + 2, shaftY + 2, OUTLINE);
        graphics.fillGradient(startX, shaftY, tipX + 1, shaftY + 1, PICK_LIGHT, PICK_DARK);
        // The hooked tip reaches up to the pins
        graphics.fill(tipX - 1, plugTop - 3, tipX + 2, shaftY + 2, OUTLINE);
        graphics.fill(tipX, plugTop - 2, tipX + 1, shaftY + 1, PICK_LIGHT);
        // Wooden handle off to the left
        graphics.fill(startX, shaftY - 2, startX + 14, shaftY + 3, OUTLINE);
        graphics.fill(startX, shaftY - 1, startX + 13, shaftY + 2, 0xFF8B5A2B);
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
        switch (keyCode) {
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> {
                move(-1);
                return true;
            }
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> {
                move(1);
                return true;
            }
            case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                attempt();
                return true;
            }
            default -> {
            }
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
