package net.luckyowlstudios.locksmith.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.init.ModEntityTypes;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.loot.LootInjections;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class LocksmithFabric implements ModInitializer {

    private static final int DUNGEON_SLIME_SPAWN_WEIGHT = 10;

    @Override
    public void onInitialize() {
        Locksmith.init();
        registerEvents();
        registerLoot();
        registerEntities();
        registerCreativeTabEntries();
    }

    private static void registerEvents() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            return switch (LockEvents.onRightClickBlock(player, level, hand, hitResult.getBlockPos())) {
                case PASS -> InteractionResult.PASS;
                // On the client, only SUCCESS sends the click on to the server, which has to run the
                // same logic to change the lock and play the sounds.
                case CANCEL -> InteractionResult.SUCCESS;
                case CANCEL_FAIL -> level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            };
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                LockEvents.canBreakBlock(player, level, pos));
        // Anvil key duplication and explosion protection are done with mixins, see fabric/mixin
    }

    private static void registerLoot() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            for (LootInjections.Injection injection : LootInjections.ALL) {
                if (injection.table().equals(key)) {
                    LootPool.Builder pool = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(injection.item().get()));
                    if (injection.chance() < 1.0F) {
                        pool.when(LootItemRandomChanceCondition.randomChance(injection.chance()));
                    }
                    tableBuilder.withPool(pool);
                }
            }
        });
    }

    private static void registerEntities() {
        FabricDefaultAttributeRegistry.register(ModEntityTypes.DUNGEON_SLIME.get(), Monster.createMonsterAttributes());
        SpawnPlacements.register(ModEntityTypes.DUNGEON_SLIME.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DungeonSlime::checkDungeonSlimeSpawnRules);
        // Keep in sync with the NeoForge biome modifier, data/locksmith/neoforge/biome_modifier/dungeon_slime.json
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntityTypes.DUNGEON_SLIME.get(),
                DUNGEON_SLIME_SPAWN_WEIGHT, 1, 1);
    }

    private static void registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
                entries.accept(ModItems.DUNGEON_SLIME_SPAWN_EGG.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries ->
                entries.addAfter(Items.CHEST, ModBlocks.IRON_CHEST.get(), ModBlocks.GOLDEN_CHEST.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.addAfter(Items.CHEST, ModBlocks.IRON_CHEST.get(), ModBlocks.GOLDEN_CHEST.get());
            entries.addAfter(Items.TRAPPED_CHEST, ModBlocks.IRON_TRAPPED_CHEST.get(), ModBlocks.GOLDEN_TRAPPED_CHEST.get());
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries ->
                entries.addAfter(Items.EXPERIENCE_BOTTLE, ModItems.KEY.get(), ModItems.GOLDEN_KEY.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> {
            entries.accept(ModItems.GOLDEN_LOCK.get());
            entries.accept(ModItems.TRIAL_LOCK.get());
        });
    }
}
