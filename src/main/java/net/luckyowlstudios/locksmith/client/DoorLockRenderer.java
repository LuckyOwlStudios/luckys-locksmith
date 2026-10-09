package net.luckyowlstudios.locksmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.network.DoorLocksPayload;
import net.luckyowlstudios.locksmith.util.LockType;
import net.luckyowlstudios.locksmith.util.DoorLockPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Locksmith.MOD_ID, value = Dist.CLIENT)
public final class DoorLockRenderer {
    private static final Map<Long, Map<BlockPos, LockType>> CHUNKS = new HashMap<>();
    private static final ModelPart MODEL = createModel();
    private static final ResourceLocation NORMAL = Locksmith.id("textures/entity/lock/chest/lock.png");
    private static final ResourceLocation GOLDEN = Locksmith.id("textures/entity/lock/chest/golden_lock.png");
    private static final ResourceLocation TRIAL = Locksmith.id("textures/entity/lock/chest/trial_lock.png");

    private static ModelPart createModel() {
        MeshDefinition mesh = new MeshDefinition();
        // Reuse the chest padlock's UVs without its chest-sized surrounding cube.
        mesh.getRoot().addOrReplaceChild("lock", CubeListBuilder.create()
                .texOffs(0, 9).addBox(-2.5F, -2, -1, 5, 4, 1)
                .texOffs(1, 6).addBox(-2, -5, -0.5F, 4, 3, 0), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    public static void accept(DoorLocksPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        if (payload.locks().isEmpty()) CHUNKS.remove(payload.chunk());
        else CHUNKS.put(payload.chunk(), payload.locks());
    }

    @SubscribeEvent
    public static void unloadChunk(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) CHUNKS.remove(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void unloadLevel(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) CHUNKS.clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CHUNKS.clear(); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || CHUNKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        for (var chunk : CHUNKS.values()) {
            for (var entry : chunk.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!mc.level.hasChunkAt(pos) || !event.getFrustum().isVisible(new AABB(pos).expandTowards(0, 1, 0).inflate(0.15))) continue;
                BlockState state = mc.level.getBlockState(pos);
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
                // A padlock on each side, slightly clear of the three-pixel-thick door panel.
                for (boolean back : new boolean[]{false, true}) {
                    pose.pushPose();
                    pose.translate(DoorLockPlacement.handleX(state), 1.05, back ? 0.80 : 1.0125);
                    if (back) pose.mulPose(Axis.YP.rotationDegrees(180));
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                    MODEL.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
                pose.popPose();
            }
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(NORMAL));
        buffers.endBatch(RenderType.entityCutoutNoCull(GOLDEN));
        buffers.endBatch(RenderType.entityCutoutNoCull(TRIAL));
    }
}
