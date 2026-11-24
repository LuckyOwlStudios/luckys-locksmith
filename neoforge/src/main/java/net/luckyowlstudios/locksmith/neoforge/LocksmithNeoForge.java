package net.luckyowlstudios.locksmith.neoforge;

import com.mojang.logging.LogUtils;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.network.DoorLocksPayload;
import net.luckyowlstudios.locksmith.util.DoorLocks;
import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.init.ModEntityTypes;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.neoforge.platform.NeoForgePlatformHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Locksmith.MOD_ID)
public class LocksmithNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public LocksmithNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        Locksmith.init();
        NeoForgePlatformHelper.registerAll(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(LocksmithNeoForge::addCreative);
        modEventBus.addListener(LocksmithNeoForge::registerAttributes);
        modEventBus.addListener(LocksmithNeoForge::registerSpawnPlacements);
        modEventBus.addListener(LocksmithNeoForge::modifyDefaultComponents);
        modEventBus.addListener(LocksmithNeoForge::registerPayloads);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, LocksmithConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (LocksmithConfig.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", LocksmithConfig.MAGIC_NUMBER_INTRODUCTION.get(), LocksmithConfig.MAGIC_NUMBER.getAsInt());

        LocksmithConfig.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Handlers run on the main thread; on the client, context.player() is the local player
        event.registrar("1").playToClient(DoorLocksPayload.TYPE, DoorLocksPayload.STREAM_CODEC,
                (payload, context) -> DoorLocks.applyClientSync(context.player().level(), payload));
    }

    private static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.TRIAL_KEY, builder -> builder.set(DataComponents.MAX_STACK_SIZE, ModItems.KEY_STACK_SIZE));
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.DUNGEON_SLIME.get(), Monster.createMonsterAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Spawn biomes come from the locksmith:dungeon_slime biome modifier
        event.register(ModEntityTypes.DUNGEON_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DungeonSlime::checkDungeonSlimeSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        ItemStack ironChest = new ItemStack(ModBlocks.IRON_CHEST.get());
        ItemStack goldenChest = new ItemStack(ModBlocks.GOLDEN_CHEST.get());
        ItemStack jeweledChest = new ItemStack(ModBlocks.JEWELED_CHEST.get());
        ItemStack ironTrappedChest = new ItemStack(ModBlocks.IRON_TRAPPED_CHEST.get());
        ItemStack goldenTrappedChest = new ItemStack(ModBlocks.GOLDEN_TRAPPED_CHEST.get());
        ItemStack jeweledTrappedChest = new ItemStack(ModBlocks.JEWELED_TRAPPED_CHEST.get());
        ItemStack key = new ItemStack(ModItems.KEY.get());
        ItemStack goldenKey = new ItemStack(ModItems.GOLDEN_KEY.get());

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.insertAfter(Items.CHEST.getDefaultInstance(), ironChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ironChest, goldenChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(goldenChest, jeweledChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.insertAfter(Items.TRAPPED_CHEST.getDefaultInstance(), ironTrappedChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(ironTrappedChest, goldenTrappedChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(goldenTrappedChest, jeweledTrappedChest, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.insertAfter(Items.EXPERIENCE_BOTTLE.getDefaultInstance(), key, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(key, goldenKey, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(new ItemStack(ModItems.DUNGEON_SLIME_SPAWN_EGG.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(new ItemStack(ModItems.GOLDEN_LOCK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(new ItemStack(ModItems.TRIAL_LOCK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
