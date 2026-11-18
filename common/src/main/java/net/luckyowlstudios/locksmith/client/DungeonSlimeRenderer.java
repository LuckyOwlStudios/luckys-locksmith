package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the dungeon slime like a vanilla slime with a gray texture. The texture leaves out the inner
 * cube, and a golden key spins in its place while the slime still carries it.
 */
public class DungeonSlimeRenderer extends SlimeRenderer {

    private static final ResourceLocation TEXTURE = Locksmith.id("textures/entity/slime/dungeon_slime.png");

    public DungeonSlimeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new KeyLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Slime entity) {
        return TEXTURE;
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
            // Model space is upside down; the slime's inner cube is centered 20 pixels down
            poseStack.translate(0.0F, 20.0F / 16.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, Math.sin(ageInTicks * 0.1F) * 0.03F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 3.0F));
            // FIXED draws the item centered at half size, which fits inside the 6 pixel inner cube
            poseStack.scale(0.7F, 0.7F, 0.7F);
            this.itemRenderer.renderStatic(this.key, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, slime.level(), slime.getId());
            poseStack.popPose();
        }
    }
}
