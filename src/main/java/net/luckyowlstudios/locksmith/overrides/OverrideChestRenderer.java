package net.luckyowlstudios.locksmith.overrides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.luckyowlstudios.locksmith.util.LockHandler;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class OverrideChestRenderer extends ChestRenderer<ChestBlockEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Locksmith.id("lock"), "main");
    private final ModelPart lock;

    public OverrideChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        ModelPart modelpart = context.bakeLayer(LAYER_LOCATION);
        this.lock = modelpart.getChild("lock");
    }

    public static LayerDefinition createLock() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition lock = partdefinition.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 9).addBox(-2.5F, -6.0F, -8.0F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(1, 6).addBox(-2.0F, -9.0F, -7.5F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 14.0F, 14.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void render(ChestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (LockHandler.containerHasLock(blockEntity)) {
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

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(getTexture(blockEntity)));
            this.lock.render(poseStack, consumer, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    public static ResourceLocation getTexture(BaseContainerBlockEntity containerBlockEntity) {
        if (containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get())) {
            return switch (containerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get())) {
                case GOLDEN -> Locksmith.id("textures/entity/lock/chest/golden_lock.png");
                case TRIAL -> Locksmith.id("textures/entity/lock/chest/trial_lock.png");
                default -> Locksmith.id("textures/entity/lock/chest/lock.png");
            };
        }
        return Locksmith.id("textures/entity/lock/chest/lock.png");
    }
}
