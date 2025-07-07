package net.luckyowlstudios.locksmith.datagen.types;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Locksmith.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        this.basicItem(ModItems.KEY.asItem());
        this.basicItem(ModItems.GOLDEN_KEY.asItem());

        this.basicItem(ModItems.GOLDEN_LOCK.asItem());
        this.basicItem(ModItems.TRIAL_LOCK.asItem());
    }
}
