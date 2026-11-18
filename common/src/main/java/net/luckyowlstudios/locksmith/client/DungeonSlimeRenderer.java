package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Draws the dungeon slime with the vanilla slime model and a gray copy of the vanilla slime texture.
 * The gray copy is made from whatever slime texture is loaded (so resource packs carry over). The inner
 * core is drawn darker and see-through so the golden key shows inside it while the slime carries it.
 */
public class DungeonSlimeRenderer extends SlimeRenderer {

    private static final ResourceLocation VANILLA_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png");
    private static final ResourceLocation GRAY_TEXTURE = Locksmith.id("dynamic/dungeon_slime");
    private static final ResourceLocation CORE_TEXTURE = Locksmith.id("dynamic/dungeon_slime_core");
    /** The core is darker than the shell and only partly see-through, so the key shows inside it. */
    private static final int CORE_ALPHA = 150;
    private static final float CORE_BRIGHTNESS = 0.55F;

    // The inner cube's area of the slime texture: a 6x6x6 box at texture offset (0, 16)
    private static final int CORE_U = 0, CORE_V = 16, CORE_WIDTH = 24, CORE_HEIGHT = 12;

    /** Whether the gray texture has been tried for this renderer; renderers are rebuilt on resource reload. */
    private boolean textureTried;
    private boolean textureReady;

    public DungeonSlimeRenderer(EntityRendererProvider.Context context) {
        super(context);
        // The key has to be drawn before the see-through outer shell, or the shell hides it
        this.layers.add(0, new KeyLayer(this, context.getItemRenderer()));
        // The vanilla core is drawn solid with the body; this draws it again see-through, after the key
        this.layers.add(1, new CoreLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Slime entity) {
        if (!this.textureTried) {
            this.textureTried = true;
            this.textureReady = createGrayTextures();
        }
        return this.textureReady ? GRAY_TEXTURE : VANILLA_TEXTURE;
    }

    /**
     * Builds two gray copies of the vanilla slime texture: the body (shell, eyes and mouth) with the
     * core cut out, and the core on its own, darker and see-through, for {@link CoreLayer}.
     */
    private static boolean createGrayTextures() {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(VANILLA_TEXTURE);
        if (resource.isEmpty()) return false;

        try (InputStream stream = resource.get().open()) {
            NativeImage body = NativeImage.read(stream);
            NativeImage core = new NativeImage(body.getWidth(), body.getHeight(), true);
            for (int x = 0; x < body.getWidth(); x++) {
                for (int y = 0; y < body.getHeight(); y++) {
                    // NativeImage pixels are ABGR
                    int pixel = body.getPixelRGBA(x, y);
                    int alpha = pixel >>> 24;
                    int red = pixel & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = (pixel >> 16) & 0xFF;
                    int gray = (int) (0.299F * red + 0.587F * green + 0.114F * blue);

                    if (isCore(x, y, body.getWidth())) {
                        body.setPixelRGBA(x, y, 0);
                        core.setPixelRGBA(x, y, grayPixel(alpha == 0 ? 0 : CORE_ALPHA, gray * CORE_BRIGHTNESS));
                    } else {
                        // Thin out the see-through shell so the inside reads clearly
                        int bodyAlpha = alpha < 255 ? (int) (alpha * 0.75F) : alpha;
                        body.setPixelRGBA(x, y, grayPixel(bodyAlpha, gray * 0.8F));
                        core.setPixelRGBA(x, y, 0);
                    }
                }
            }
            minecraft.getTextureManager().register(GRAY_TEXTURE, new DynamicTexture(body));
            minecraft.getTextureManager().register(CORE_TEXTURE, new DynamicTexture(core));
            return true;
        } catch (IOException e) {
            Locksmith.LOGGER.error("Couldn't create the dungeon slime textures", e);
            return false;
        }
    }

    private static int grayPixel(int alpha, float gray) {
        int value = Math.min(255, (int) gray);
        return (alpha << 24) | (value << 16) | (value << 8) | value;
    }

    private static boolean isCore(int x, int y, int imageWidth) {
        // Scale for higher resolution resource packs; vanilla is 64 pixels wide
        float scale = imageWidth / 64.0F;
        return x >= CORE_U * scale && x < (CORE_U + CORE_WIDTH) * scale
                && y >= CORE_V * scale && y < (CORE_V + CORE_HEIGHT) * scale;
    }

    private class CoreLayer extends RenderLayer<Slime, SlimeModel<Slime>> {

        CoreLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Slime slime,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (slime.isInvisible() || !DungeonSlimeRenderer.this.textureReady) return;
            // Uses the body model, whose texture here has only the core filled in
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(CORE_TEXTURE));
            this.getParentModel().renderToBuffer(poseStack, consumer, packedLight, LivingEntityRenderer.getOverlayCoords(slime, 0.0F));
        }
    }

    private static class KeyLayer extends RenderLayer<Slime, SlimeModel<Slime>> {
        private static final RenderType KEY_RENDER_TYPE = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
        private final ItemRenderer itemRenderer;
        private ItemStack key;

        KeyLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent, ItemRenderer itemRenderer) {
            super(parent);
            this.itemRenderer = itemRenderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Slime slime,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(slime instanceof DungeonSlime dungeonSlime) || !dungeonSlime.hasKey() || slime.isInvisible()) return;
            if (this.key == null) {
                // Items aren't registered yet when renderers are created, so look the key up lazily
                this.key = new ItemStack(ModItems.GOLDEN_KEY.get());
            }

            poseStack.pushPose();
            // Model space is upside down; the slime's inner cube is centered 20 pixels down. The key
            // sits still in the middle of the body, facing the slime's front.
            poseStack.translate(0.0F, 20.0F / 16.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            // Undo the slime's size scaling so the key is drawn at the same size as a dropped item
            float size = slime.getSize();
            poseStack.scale(1.0F / size, 1.0F / size, 1.0F / size);
            // GROUND lifts items 2 pixels above their origin; move it back down to center the key
            poseStack.translate(0.0F, -2.0F / 16.0F, 0.0F);
            // Item render types (including entityCutout on the block atlas) use fixed buffers that are
            // drawn after every entity, so the translucent shell would already have written depth and
            // hidden the key. entityCutoutNoCull on the atlas isn't a fixed buffer: it's drawn as soon
            // as the shell asks for its own buffer, so the shell then blends over the key.
            MultiBufferSource keyBuffer = renderType -> buffer.getBuffer(KEY_RENDER_TYPE);
            this.itemRenderer.renderStatic(this.key, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, keyBuffer, slime.level(), slime.getId());
            poseStack.popPose();
        }
    }
}
