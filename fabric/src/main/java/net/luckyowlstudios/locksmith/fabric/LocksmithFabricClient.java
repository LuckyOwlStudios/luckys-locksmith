package net.luckyowlstudios.locksmith.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.jeweled.JeweledChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.jeweled.JeweledChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.jeweled_trapped.JeweledTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.jeweled_trapped.JeweledTrappedChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestRenderer;
import net.luckyowlstudios.locksmith.client.DoorLockRenderer;
import net.luckyowlstudios.locksmith.network.DoorLocksPayload;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.client.DungeonSlimeRenderer;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.luckyowlstudios.locksmith.init.ModEntityTypes;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.overrides.AddBarrelRenderer;
import net.luckyowlstudios.locksmith.overrides.LockModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class LocksmithFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(LockModel.LAYER_LOCATION, LockModel::createLock);
        EntityModelLayerRegistry.registerModelLayer(AddBarrelRenderer.LAYER_LOCATION, AddBarrelRenderer::createBodyLayer);

        registerRenderer(ModBlockEntityTypes.IRON_CHEST_BLOCK_ENTITY.get(), IronChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.GOLDEN_CHEST_BLOCK_ENTITY.get(), GoldenChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.JEWELED_CHEST_BLOCK_ENTITY.get(), JeweledChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.IRON_TRAPPED_CHEST_BLOCK_ENTITY.get(), IronTrappedChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY.get(), GoldenTrappedChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.JEWELED_TRAPPED_CHEST_BLOCK_ENTITY.get(), JeweledTrappedChestRenderer::new);
        registerRenderer(BlockEntityType.BARREL, AddBarrelRenderer::new);

        EntityRendererRegistry.register(ModEntityTypes.DUNGEON_SLIME.get(), DungeonSlimeRenderer::new);

        registerItemRenderer(ModBlocks.IRON_CHEST.get(), IronChestItemRenderer::new);
        registerItemRenderer(ModBlocks.GOLDEN_CHEST.get(), GoldenChestItemRenderer::new);
        registerItemRenderer(ModBlocks.JEWELED_CHEST.get(), JeweledChestItemRenderer::new);
        registerItemRenderer(ModBlocks.IRON_TRAPPED_CHEST.get(), IronTrappedChestItemRenderer::new);
        registerItemRenderer(ModBlocks.GOLDEN_TRAPPED_CHEST.get(), GoldenTrappedChestItemRenderer::new);
        registerItemRenderer(ModBlocks.JEWELED_TRAPPED_CHEST.get(), JeweledTrappedChestItemRenderer::new);

        // Runs on the client thread
        ClientPlayNetworking.registerGlobalReceiver(DoorLocksPayload.TYPE,
                (payload, context) -> DoorLocks.applyClientSync(context.player().level(), payload));

        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                DoorLockRenderer.render(context.matrixStack(), context.camera().getPosition(), context.frustum()));

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> LockEvents.appendTooltip(stack, lines));
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity> void registerRenderer(BlockEntityType<? extends E> type, BlockEntityRendererProvider<E> provider) {
        BlockEntityRendererRegistry.register((BlockEntityType<E>) type, provider);
    }

    private static void registerItemRenderer(ItemLike item, Supplier<BlockEntityWithoutLevelRenderer> factory) {
        // The item renderers look up the block entity renderer, which doesn't exist yet while mods
        // initialize, so create each one the first time it draws.
        BlockEntityWithoutLevelRenderer[] renderer = new BlockEntityWithoutLevelRenderer[1];
        BuiltinItemRendererRegistry.INSTANCE.register(item, (stack, displayContext, poseStack, bufferSource, light, overlay) -> {
            if (renderer[0] == null) {
                renderer[0] = factory.get();
            }
            renderer[0].renderByItem(stack, displayContext, poseStack, bufferSource, light, overlay);
        });
    }
}
