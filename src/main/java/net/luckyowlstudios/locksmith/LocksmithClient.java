package net.luckyowlstudios.locksmith;

import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestItemRenderer;
import net.luckyowlstudios.locksmith.block.chest.iron.IronChestRenderer;
import net.luckyowlstudios.locksmith.init.ModBlockEntityTypes;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.block.chest.gold.GoldenChestItemRenderer;
import net.luckyowlstudios.locksmith.overrides.AddBarrelRenderer;
import net.luckyowlstudios.locksmith.overrides.OverrideChestRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@EventBusSubscriber(modid = Locksmith.MOD_ID, value = Dist.CLIENT)
public class LocksmithClient {
    public LocksmithClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityType.CHEST, OverrideChestRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityType.TRAPPED_CHEST, OverrideChestRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityType.BARREL, AddBarrelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.IRON_CHEST_BLOCK_ENTITY.get(), IronChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.GOLDEN_CHEST_BLOCK_ENTITY.get(), GoldenChestRenderer::new);
    }

    @SubscribeEvent
    public static void registerBERI(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new IronChestItemRenderer();
            }
        }, ModBlocks.IRON_CHEST.asItem());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new GoldenChestItemRenderer();
            }
        }, ModBlocks.GOLDEN_CHEST.asItem());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OverrideChestRenderer.LAYER_LOCATION, OverrideChestRenderer::createLock);
        event.registerLayerDefinition(AddBarrelRenderer.LAYER_LOCATION, AddBarrelRenderer::createBodyLayer);
    }
}