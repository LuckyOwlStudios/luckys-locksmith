package net.luckyowlstudios.locksmith.util;

import net.luckyowlstudios.locksmith.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public class LockHandler {



    public static boolean containerHasLock(BaseContainerBlockEntity containerBlockEntity) {
        return containerBlockEntity.components().has(DataComponents.LOCK) || (containerBlockEntity.components().has(ModDataComponents.LOCK_TYPE.get()) && containerBlockEntity.components().get(ModDataComponents.LOCK_TYPE.get()) != LockType.NONE);
    }
}
