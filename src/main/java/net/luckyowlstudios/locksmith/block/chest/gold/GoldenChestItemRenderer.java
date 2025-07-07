package net.luckyowlstudios.locksmith.block.chest.gold;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestBlockEntity;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class GoldenChestItemRenderer extends BlockEntityWithoutLevelRenderer {

    public GoldenChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(new GoldenChestBlockEntity(BlockPos.ZERO, ModBlocks.GOLDEN_CHEST.get().defaultBlockState()), poseStack, buffer, packedLight, packedOverlay);
    }
}
