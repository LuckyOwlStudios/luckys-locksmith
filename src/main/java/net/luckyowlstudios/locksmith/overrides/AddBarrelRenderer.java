package net.luckyowlstudios.locksmith.overrides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AddBarrelRenderer<T extends BlockEntity> implements BlockEntityRenderer<BarrelBlockEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Locksmith.id("barrel_lock"), "main");
    private final ModelPart lock;

    public AddBarrelRenderer(BlockEntityRendererProvider.Context context) {
        super();
        ModelPart modelpart = context.bakeLayer(LAYER_LOCATION);
        this.lock = modelpart.getChild("lock");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition lock = partdefinition.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -16.0F, -8.0F, 5.0F, 16.0F, 16.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition cube_r1 = lock.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(12, 32).addBox(-2.0F, -4.0F, -9.5F, 4.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 32).addBox(-2.5F, -2.0F, -10.0F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, -2.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition cube_r2 = lock.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -8.0F, -8.0F, 5.0F, 16.0F, 16.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void render(BarrelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.components().has(DataComponents.LOCK)) {
            poseStack.pushPose();

            BlockState state = blockEntity.getBlockState();
            Direction facing = state.getValue(BarrelBlock.FACING);
            Direction.Axis axis = facing.getAxis();

            boolean isUpsideDown = facing == Direction.DOWN;

            // Set in place
            poseStack.translate(0.5F, isUpsideDown ? -0.5F : 1.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(isUpsideDown ? 0 : 180));

            if (axis != Direction.Axis.Y) {
                if (facing == Direction.NORTH) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.translate(0.0F, -1.0F, 1.0F);
                } else if (facing == Direction.SOUTH) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.translate(0.0F, -1.0F, -1.0F);
                } else if (facing == Direction.WEST) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                    poseStack.translate(-1.0F, -1.0F, 0.0F);
                } else if (facing == Direction.EAST) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    poseStack.translate(1.0F, -1.0F, 0.0F);
                }
            }

            float f = blockEntity.getBlockState().getValue(BarrelBlock.FACING).toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(f));
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(Locksmith.id("textures/entity/lock/barrel/lock.png")));
            int light = getFrontLightLevel(blockEntity);
            this.lock.render(poseStack, consumer, light, packedOverlay);
            poseStack.popPose();
        }
    }

    private int getFrontLightLevel(BarrelBlockEntity blockEntity) {
        var level = blockEntity.getLevel();
        if (level == null) return 15728880; // Full light fallback for safety

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(BarrelBlock.FACING);
        BlockPos frontPos = blockEntity.getBlockPos().relative(facing);
        BlockState frontState = level.getBlockState(frontPos);

        // If the block in front is solid (blocks light), fallback to barrel's position
        boolean frontOpaque = frontState.isSolidRender(level, frontPos);

        BlockPos lightSamplePos = frontOpaque
                ? blockEntity.getBlockPos() // sample from barrel instead
                : frontPos;

        int blockLight = level.getBrightness(LightLayer.BLOCK, lightSamplePos);
        int skyLight = level.getBrightness(LightLayer.SKY, lightSamplePos);
        return blockLight | (skyLight << 20);
    }
}
