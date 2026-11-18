package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.item.GoldenKey;
import net.luckyowlstudios.locksmith.item.LockItem;
import net.luckyowlstudios.locksmith.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {

    public static final Supplier<Item> KEY = register("key", () -> Services.PLATFORM.createKeyItem(new Item.Properties()));
    public static final Supplier<Item> GOLDEN_KEY = register("golden_key", () -> new GoldenKey(new Item.Properties()));

    public static final Supplier<Item> GOLDEN_LOCK = register("golden_lock", () -> new LockItem(new Item.Properties()));
    public static final Supplier<Item> TRIAL_LOCK = register("trial_lock", () -> new LockItem(new Item.Properties()));

    static <T extends Item> Supplier<T> register(String name, Supplier<T> item) {
        return Services.PLATFORM.register(BuiltInRegistries.ITEM, name, item);
    }

    public static void init() {
    }
}
