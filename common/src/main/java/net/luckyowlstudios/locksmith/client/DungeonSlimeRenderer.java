package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
 * The gray copy is made from whatever slime texture is loaded (so resource packs carry over), with the
 * inner core left out so the golden key shows in its place while the slime still carries it.
 */
public class DungeonSlimeRenderer extends SlimeRenderer {

    private static final ResourceLocation VANILLA_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png");
    private static final ResourceLocation GRAY_TEXTURE = Locksmith.id("dynamic/dungeon_slime");

    // The inner cube's area of the slime texture: a 6x6x6 box at texture offset (0, 16)
    private static final int CORE_U = 0, CORE_V = 16, CORE_WIDTH = 24, CORE_HEIGHT = 12;

    /** Whether the gray texture has been tried for this renderer; renderers are rebuilt on resource reload. */
    private boolean textureTried;
    private boolean textureReady;

    public DungeonSlimeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new KeyLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Slime entity) {
        if (!this.textureTried) {
            this.textureTried = true;
            this.textureReady = createGrayTexture();
        }
        return this.textureReady ? GRAY_TEXTURE : VANILLA_TEXTURE;
    }

    private static boolean createGrayTexture() {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(VANILLA_TEXTURE);
        if (resource.isEmpty()) return false;

        try (InputStream stream = resource.get().open()) {
            NativeImage image = NativeImage.read(stream);
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    // NativeImage pixels are ABGR
                    int pixel = image.getPixelRGBA(x, y);
                    int alpha = pixel >>> 24;
                    if (isCore(x, y, image.getWidth())) {
                        alpha = 0;
                    }
                    int red = pixel & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = (pixel >> 16) & 0xFF;
                    int gray = (int) ((0.299F * red + 0.587F * green + 0.114F * blue) * 0.8F);
                    image.setPixelRGBA(x, y, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
            minecraft.getTextureManager().register(GRAY_TEXTURE, new DynamicTexture(image));
            return true;
        } catch (IOException e) {
            Locksmith.LOGGER.error("Couldn't create the dungeon slime texture", e);
            return false;
        }
    }

    private static boolean isCore(int x, int y, int imageWidth) {
        // Scale for higher resolution resource packs; vanilla is 64 pixels wide
        float scale = imageWidth / 64.0F;
        return x >= CORE_U * scale && x < (CORE_U + CORE_WIDTH) * scale
                && y >= CORE_V * scale && y < (CORE_V + CORE_HEIGHT) * scale;
    }

    private static class KeyLayer extends RenderLayer<Slime, SlimeModel<Slime>> {
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
            this.itemRenderer.renderStatic(this.key, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, slime.level(), slime.getId());
            poseStack.popPose();
        }
    }
}
