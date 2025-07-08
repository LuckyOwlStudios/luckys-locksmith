package net.luckyowlstudios.locksmith.datagen.types;

import net.luckyowlstudios.locksmith.init.ModBlocks;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.KEY.get())
                .pattern("@")
                .pattern("#")
                .pattern("#")
                .define('@', Items.IRON_INGOT)
                .define('#', Items.IRON_NUGGET)
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GOLDEN_KEY.get())
                .pattern("@")
                .pattern("#")
                .pattern("#")
                .define('@', Items.GOLD_INGOT)
                .define('#', Items.GOLD_NUGGET)
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_gold", has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.IRON_CHEST.get())
                .pattern("@#@")
                .pattern("#$#")
                .pattern("@#@")
                .define('@', Items.IRON_BLOCK)
                .define('#', Items.BLACK_WOOL)
                .define('$', Items.CHEST)
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_iron", has(Items.IRON_BLOCK))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.GOLDEN_CHEST.get())
                .pattern("@#@")
                .pattern("#$#")
                .pattern("@#@")
                .define('@', Items.GOLD_BLOCK)
                .define('#', Items.RED_WOOL)
                .define('$', Items.CHEST)
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_gold", has(Items.GOLD_BLOCK))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.IRON_TRAPPED_CHEST.get())
                .requires(ModBlocks.IRON_CHEST)
                .requires(Ingredient.of(Items.TRIPWIRE_HOOK))
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_tripwire", has(Items.TRIPWIRE_HOOK))
                .save(recipeOutput, ModBlocks.IRON_TRAPPED_CHEST.getId());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.GOLDEN_TRAPPED_CHEST.get())
                .requires(ModBlocks.GOLDEN_CHEST)
                .requires(Ingredient.of(Items.TRIPWIRE_HOOK))
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .unlockedBy("has_tripwire", has(Items.TRIPWIRE_HOOK))
                .save(recipeOutput, ModBlocks.GOLDEN_TRAPPED_CHEST.getId());
    }
}
