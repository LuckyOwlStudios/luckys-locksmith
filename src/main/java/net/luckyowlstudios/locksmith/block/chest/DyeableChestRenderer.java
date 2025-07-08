package net.luckyowlstudios.locksmith.block.chest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public abstract class DyeableChestRenderer extends ChestRenderer<ChestBlockEntity> {

    protected final ModelPart chest;
    protected final ModelPart left;
    protected final ModelPart right;

    public DyeableChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        chest = context.bakeLayer(ModelLayers.CHEST);
        left = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        right = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
    }

    protected abstract DyedItemColor defaultColor();

    @Override
    public void render(ChestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        DyedItemColor dyedComponent = blockEntity.components().getOrDefault(DataComponents.DYED_COLOR, defaultColor());
        int color = dyedComponent.rgb();
        Level level = blockEntity.getLevel();
        boolean flag = level != null;
        BlockState blockstate = flag ? blockEntity.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        ChestType chesttype = blockstate.hasProperty(ChestBlock.TYPE) ? blockstate.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        Block var12 = blockstate.getBlock();
        if (var12 instanceof AbstractChestBlock<?> abstractchestblock) {
            boolean flag1 = chesttype != ChestType.SINGLE;
            poseStack.pushPose();
            float f = blockstate.getValue(ChestBlock.FACING).toYRot();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-f));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> neighborcombineresult;
            if (flag) {
                neighborcombineresult = abstractchestblock.combine(blockstate, level, blockEntity.getBlockPos(), true);
            } else {
                neighborcombineresult = DoubleBlockCombiner.Combiner::acceptNone;
            }

            float f1 = neighborcombineresult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTick);
            f1 = 1.0F - f1;
            f1 = 1.0F - f1 * f1 * f1;
            int i = ((Int2IntFunction)neighborcombineresult.apply(new BrightnessCombiner())).applyAsInt(packedLight);
            Material existingMaterial = this.getMaterial(blockEntity, chesttype);
            ResourceLocation dyeOverlay = switch (chesttype) {
                case LEFT -> ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/dye_left");
                case RIGHT -> ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/dye_right");
                case SINGLE -> ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/dye");
            };
            Material dyeMaterial = new Material(existingMaterial.atlasLocation(), dyeOverlay);
            VertexConsumer vertexconsumer = dyeMaterial.buffer(bufferSource, RenderType::entityCutout);
            if (flag1) {
                if (chesttype == ChestType.LEFT) {
                    this.render(poseStack, vertexconsumer, left.getChild("lid"), left.getChild("lock"), left.getChild("bottom"), f1, i, packedOverlay, color);
                } else {
                    this.render(poseStack, vertexconsumer, right.getChild("lid"), right.getChild("lock"), right.getChild("bottom"), f1, i, packedOverlay, color);
                }
            } else {
                this.render(poseStack, vertexconsumer, chest.getChild("lid"), chest.getChild("lock"), chest.getChild("bottom"), f1, i, packedOverlay, color);
            }
            poseStack.popPose();
        }
    }

    private void render(PoseStack poseStack, VertexConsumer consumer, ModelPart lidPart, ModelPart lockPart, ModelPart bottomPart, float lidAngle, int packedLight, int packedOverlay, int color) {
        lidPart.xRot = -(lidAngle * ((float)Math.PI / 2F));
        lockPart.xRot = lidPart.xRot;
        lidPart.render(poseStack, consumer, packedLight, packedOverlay, color);
        lockPart.render(poseStack, consumer, packedLight, packedOverlay, color);
        bottomPart.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    @Override
    protected Material getMaterial(ChestBlockEntity blockEntity, ChestType chestType) {
        if (blockEntity.getBlockState().getBlock() == ModBlocks.IRON_CHEST.get()) {
            return switch (chestType) {
                case LEFT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron_left"));
                case RIGHT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron_right"));
                case SINGLE -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron"));
            };
        }
        return switch (chestType) {
            case LEFT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden_left"));
            case RIGHT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden_right"));
            case SINGLE -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden"));
        };
    }
}
