package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.entity.DungeonSlime;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntityTypes {

    // Same dimensions as a vanilla slime; the size multiplier scales them
    public static final Supplier<EntityType<DungeonSlime>> DUNGEON_SLIME = Services.PLATFORM.register(BuiltInRegistries.ENTITY_TYPE, "dungeon_slime",
            () -> EntityType.Builder.of(DungeonSlime::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .eyeHeight(0.325F)
                    .spawnDimensionsScale(4.0F)
                    .clientTrackingRange(10)
                    .build("dungeon_slime"));

    public static void init() {
    }
}
