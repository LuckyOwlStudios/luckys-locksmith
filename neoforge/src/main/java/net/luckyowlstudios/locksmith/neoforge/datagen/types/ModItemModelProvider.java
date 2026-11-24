package net.luckyowlstudios.locksmith.neoforge.datagen.types;

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
        this.basicItem(ModItems.KEY.get());
        this.basicItem(ModItems.GOLDEN_KEY.get());

        this.handheldItem(ModItems.LOCK_PICK.get());

        this.basicItem(ModItems.GOLDEN_LOCK.get());
        this.basicItem(ModItems.TRIAL_LOCK.get());
    }
}
