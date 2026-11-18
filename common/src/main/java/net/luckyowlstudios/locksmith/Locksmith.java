package net.luckyowlstudios.locksmith;

import com.mojang.logging.LogUtils;
import net.luckyowlstudios.locksmith.init.*;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Shared entry point. Each loader module calls {@link #init()} from its own entry point.
 */
public class Locksmith {
    public static final String MOD_ID = "locksmith";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Loading Locksmith on {}", Services.PLATFORM.getPlatformName());
        // Order matters on Fabric, where registration happens immediately: blocks before the block
        // entity types that reference them, and entity types before the spawn eggs that reference them.
        ModDataComponents.init();
        ModBlocks.init();
        ModEntityTypes.init();
        ModItems.init();
        ModBlockEntityTypes.init();
        ModCreativeModeTabs.init();
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }
}
