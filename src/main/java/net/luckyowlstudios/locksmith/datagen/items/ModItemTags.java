package net.luckyowlstudios.locksmith.datagen.items;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags {
    public static final TagKey<Item> KEYS = createTag("keys");

    private static TagKey<Item> createTag(String name) {
        return ItemTags.create(Locksmith.id(name));
    }
}
