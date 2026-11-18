package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.Objects;

/**
 * Draws the padlock on the front of a locked chest. Used by the vanilla chest renderer (through a
 * mixin) and by this mod's own chest renderers.
 */
public final class ChestLockRenderer {

    private ChestLockRenderer() {
    }

    public static void render(ModelPart lock, ChestBlockEntity chestBlockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!LockHandler.containerHasLock(chestBlockEntity)) return;

        BlockState blockState = chestBlockEntity.getBlockState();
        if (!blockState.hasProperty(ChestBlock.TYPE) || !blockState.hasProperty(ChestBlock.FACING)) return;
        ChestType chestType = blockState.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.LEFT) return;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        float f = blockState.getValue(ChestBlock.FACING).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(f));

        // Adjust if Double Chest
        if (chestType != ChestType.SINGLE) poseStack.translate(0.5F, 0.0F, 0.0F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(Objects.requireNonNull(getTexture(chestBlockEntity))));
        lock.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static ResourceLocation getTexture(BaseContainerBlockEntity containerBlockEntity) {
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
