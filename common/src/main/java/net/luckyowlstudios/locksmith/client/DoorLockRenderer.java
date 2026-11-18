package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.util.DoorLockPlacement;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.luckyowlstudios.locksmith.util.LockType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Draws the padlock on both sides of every locked door the client knows about. Each loader calls
 * {@link #render} once per frame after block entities are drawn. Doors have no block entity, so
 * this is how their lock gets drawn.
 */
public final class DoorLockRenderer {
    /** Height of the padlock's origin above the bottom of the door: 1.05 blocks, lowered by 3 pixels. */
    private static final double PADLOCK_HEIGHT = 1.05 - 3.0 / 16.0;
    private static final ModelPart MODEL = createModel();
    private static final ResourceLocation NORMAL = Locksmith.id("textures/entity/lock/chest/lock.png");
    private static final ResourceLocation GOLDEN = Locksmith.id("textures/entity/lock/chest/golden_lock.png");
    private static final ResourceLocation TRIAL = Locksmith.id("textures/entity/lock/chest/trial_lock.png");

    private DoorLockRenderer() {
    }

    private static ModelPart createModel() {
        MeshDefinition mesh = new MeshDefinition();
        // Reuse the chest padlock's UVs without its chest-sized surrounding cube.
        mesh.getRoot().addOrReplaceChild("lock", CubeListBuilder.create()
                .texOffs(0, 9).addBox(-2.5F, -2, -1, 5, 4, 1)
                .texOffs(1, 6).addBox(-2, -5, -0.5F, 4, 3, 0), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    /** @param pose the level's pose stack, not yet moved to the camera */
    public static void render(PoseStack pose, Vec3 camera, Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drewAny = false;
        for (Map<BlockPos, LockType> chunk : DoorLocks.clientChunks(mc.level)) {
            for (Map.Entry<BlockPos, LockType> entry : chunk.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!mc.level.hasChunkAt(pos) || !frustum.isVisible(new AABB(pos).expandTowards(0, 1, 0).inflate(0.15))) continue;
                BlockState state = mc.level.getBlockState(pos);
                // Trapdoors have no lock drawn yet
                if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                ResourceLocation texture = switch (entry.getValue()) {
                    case GOLDEN -> GOLDEN;
                    case TRIAL -> TRIAL;
                    default -> NORMAL;
                };
                int light = LevelRenderer.getLightColor(mc.level, pos.above());
                pose.pushPose();
                pose.translate(pos.getX() - camera.x + 0.5, pos.getY() - camera.y, pos.getZ() - camera.z + 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(DoorLockPlacement.rotation(state)));
                pose.translate(-0.5, 0, -0.5);
                // An open door swings around its hinge, and the padlocks swing with it
                double pivotX = DoorLockPlacement.hingePivotX(state);
                pose.translate(pivotX, 0, DoorLockPlacement.HINGE_PIVOT_Z);
                pose.mulPose(Axis.YP.rotationDegrees(DoorLockPlacement.openAngle(state)));
                pose.translate(-pivotX, 0, -DoorLockPlacement.HINGE_PIVOT_Z);
                // A padlock on each side, slightly clear of the three-pixel-thick door panel.
                for (boolean back : new boolean[]{false, true}) {
                    pose.pushPose();
                    pose.translate(DoorLockPlacement.handleX(state), PADLOCK_HEIGHT, back ? 0.80 : 1.0125);
                    if (back) pose.mulPose(Axis.YP.rotationDegrees(180));
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                    MODEL.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
                pose.popPose();
                drewAny = true;
            }
        }
        if (drewAny) {
            buffers.endBatch(RenderType.entityCutoutNoCull(NORMAL));
            buffers.endBatch(RenderType.entityCutoutNoCull(GOLDEN));
            buffers.endBatch(RenderType.entityCutoutNoCull(TRIAL));
        }
    }
}
