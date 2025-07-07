package net.luckyowlstudios.locksmith.init;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.item.GoldenKey;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.luckyowlstudios.locksmith.item.LockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Locksmith.MOD_ID);

    public static final DeferredItem<Item> KEY = ITEMS.register("key", () -> new KeyItem(new Item.Properties()));
    public static final DeferredItem<Item> GOLDEN_KEY = ITEMS.register("golden_key", () -> new GoldenKey(new Item.Properties()));

    public static final DeferredItem<Item> GOLDEN_LOCK = ITEMS.register("golden_lock", () -> new LockItem(new Item.Properties()));
    public static final DeferredItem<Item> TRIAL_LOCK = ITEMS.register("trial_lock", () -> new LockItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
