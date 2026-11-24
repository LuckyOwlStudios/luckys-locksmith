package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.item.GoldenKey;
import net.luckyowlstudios.locksmith.item.LockItem;
import net.luckyowlstudios.locksmith.item.LockPickItem;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {

    /** Max stack size for golden keys and vanilla trial keys. Each loader applies it to the trial key. */
    public static final int KEY_STACK_SIZE = 16;

    public static final Supplier<Item> KEY = register("key", () -> Services.PLATFORM.createKeyItem(new Item.Properties()));
    public static final Supplier<Item> GOLDEN_KEY = register("golden_key", () -> new GoldenKey(new Item.Properties().stacksTo(KEY_STACK_SIZE)));

    public static final Supplier<Item> LOCK_PICK = register("lock_pick", () -> new LockPickItem(new Item.Properties()));

    public static final Supplier<Item> GOLDEN_LOCK = register("golden_lock", () -> new LockItem(new Item.Properties()));
    public static final Supplier<Item> TRIAL_LOCK = register("trial_lock", () -> new LockItem(new Item.Properties()));

    public static final Supplier<Item> DUNGEON_SLIME_SPAWN_EGG = register("dungeon_slime_spawn_egg",
            () -> Services.PLATFORM.createSpawnEgg(ModEntityTypes.DUNGEON_SLIME, 0x7A7A7A, 0xF2C230, new Item.Properties()));

    static <T extends Item> Supplier<T> register(String name, Supplier<T> item) {
        return Services.PLATFORM.register(BuiltInRegistries.ITEM, name, item);
    }

    public static void init() {
    }
}
