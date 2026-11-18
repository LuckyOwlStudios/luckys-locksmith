package net.luckyowlstudios.locksmith.fabric.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AnvilMenu.class)
public interface AnvilMenuAccessor {

    @Accessor("cost")
    DataSlot locksmith$getCost();

    @Accessor("repairItemCountCost")
    void locksmith$setRepairItemCountCost(int repairItemCountCost);
}
