package net.luckyowlstudios.locksmith.datagen.types;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.datagen.loot.AddItemModifier;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Locksmith.MOD_ID);
    }

    @Override
    protected void start() {

        // Making piglin brutes drop a golden key
        this.add("golden_key_to_piglin_brute",
                new AddItemModifier(new LootItemCondition[] {
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/piglin_brute")).build()
                }, ModItems.GOLDEN_KEY.get().asItem()));

        // Making nether fortress chests contain a golden key
        this.add("golden_key_to_nether_fortress_chest",
                new AddItemModifier(new LootItemCondition[] {
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/nether_bridge")).build()
                }, ModItems.GOLDEN_KEY.get().asItem()));
    }
}
