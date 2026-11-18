package net.luckyowlstudios.locksmith.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.gold_trapped.GoldenTrappedChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron_trapped.IronTrappedChestRenderer;
import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
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
        registerRenderer(ModBlockEntityTypes.IRON_TRAPPED_CHEST_BLOCK_ENTITY.get(), IronTrappedChestRenderer::new);
        registerRenderer(ModBlockEntityTypes.GOLDEN_TRAPPED_CHEST_BLOCK_ENTITY.get(), GoldenTrappedChestRenderer::new);
        registerRenderer(BlockEntityType.BARREL, AddBarrelRenderer::new);

        registerItemRenderer(ModBlocks.IRON_CHEST.get(), IronChestItemRenderer::new);
        registerItemRenderer(ModBlocks.GOLDEN_CHEST.get(), GoldenChestItemRenderer::new);
        registerItemRenderer(ModBlocks.IRON_TRAPPED_CHEST.get(), IronTrappedChestItemRenderer::new);
        registerItemRenderer(ModBlocks.GOLDEN_TRAPPED_CHEST.get(), GoldenTrappedChestItemRenderer::new);

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
