package net.luckyowlstudios.locksmith.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.event.LockEvents;
import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.loot.LootChestLocks;
import net.luckyowlstudios.locksmith.loot.LootInjections;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class LocksmithFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Locksmith.init();
        registerEvents();
        registerLoot();
        registerCreativeTabEntries();
        DefaultItemComponentEvents.MODIFY.register(context ->
                context.modify(Items.TRIAL_KEY, builder -> builder.set(DataComponents.MAX_STACK_SIZE, ModItems.KEY_STACK_SIZE)));
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
        // Fabric has no config screen yet, so it uses the default chance
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                LootChestLocks.onChunkLoad(chunk, LootChestLocks.DEFAULT_FORTRESS_CHEST_LOCK_CHANCE));

        // Anvil key duplication and explosion protection are done with mixins, see fabric/mixin
    }

    private static void registerLoot() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            for (LootInjections.Injection injection : LootInjections.ALL) {
                if (injection.table().equals(key)) {
                    tableBuilder.withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1))
                            .add(LootItem.lootTableItem(injection.item().get())));
                }
            }
        });
    }

    private static void registerCreativeTabEntries() {
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
