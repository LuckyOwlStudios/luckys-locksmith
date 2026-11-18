package net.luckyowlstudios.locksmith.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckyowlstudios.locksmith.client.ChestLockRenderer;
import net.luckyowlstudios.locksmith.overrides.LockModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a padlock on locked vanilla chests.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T> {

    @Unique
    private ModelPart locksmith$lock;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void locksmith$initRenderer(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        ModelPart modelPart = context.bakeLayer(LockModel.LAYER_LOCATION);
        this.locksmith$lock = modelPart.getChild("lock");
    }

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("TAIL"))
    private void locksmith$renderLock(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, CallbackInfo ci) {
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            ChestLockRenderer.render(this.locksmith$lock, chestBlockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}
