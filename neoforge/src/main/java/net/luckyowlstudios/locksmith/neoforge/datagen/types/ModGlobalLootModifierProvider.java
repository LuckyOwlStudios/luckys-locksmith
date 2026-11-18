package net.luckyowlstudios.locksmith.neoforge.datagen.types;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.loot.LootInjections;
import net.luckyowlstudios.locksmith.neoforge.AddItemModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Locksmith.MOD_ID);
    }

    @Override
    protected void start() {
        // The list is shared with Fabric, which adds the same items with its loot table event.
        for (LootInjections.Injection injection : LootInjections.ALL) {
            List<LootItemCondition> conditions = new ArrayList<>();
            conditions.add(new LootTableIdCondition.Builder(injection.table().location()).build());
            if (injection.chance() < 1.0F) {
                conditions.add(LootItemRandomChanceCondition.randomChance(injection.chance()).build());
            }
            this.add(injection.name(), new AddItemModifier(conditions.toArray(LootItemCondition[]::new), injection.item().get()));
        }
    }
}
