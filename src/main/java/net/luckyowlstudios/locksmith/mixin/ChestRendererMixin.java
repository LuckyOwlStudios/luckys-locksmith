package net.luckyowlstudios.locksmith.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.overrides.LockModel;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

// There is a bug where players can just place a chest next to an existing LOCKED chest and convert it to a double chest, which than allows them to open locked chest. This stops this from happening!
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
        if (!(blockEntity instanceof ChestBlockEntity chestBlockEntity)) return;
        if (LockHandler.containerHasLock(chestBlockEntity)) {
            BlockState blockState = blockEntity.getBlockState();
            ChestType chestType = blockState.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.LEFT) return;
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            float f = blockEntity.getBlockState().getValue(ChestBlock.FACING).toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(f));

            // Adjust if Double Chest
            if (chestType != ChestType.SINGLE) poseStack.translate(0.5F, 0.0F, 0.0F);

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(Objects.requireNonNull(luckys_locksmith$getTexture(chestBlockEntity))));
            this.locksmith$lock.render(poseStack, consumer, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    @Unique
    private static ResourceLocation luckys_locksmith$getTexture(BaseContainerBlockEntity containerBlockEntity) {
        if (containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
            return switch (containerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get())) {
                case GOLDEN -> Locksmith.id("textures/entity/lock/chest/golden_lock.png");
                case TRIAL -> Locksmith.id("textures/entity/lock/chest/trial_lock.png");
                case null -> null;
                default -> Locksmith.id("textures/entity/lock/chest/lock.png");
            };
        }
        return Locksmith.id("textures/entity/lock/chest/lock.png");
    }
}
