package net.luckyowlstudios.locksmith.neoforge.compat;

import net.luckyowlstudios.locksmith.init.ModItems;
import net.luckyowlstudios.locksmith.item.KeyItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

/**
 * Everything that touches the Curios API. Only loaded when Curios is installed.
 */
public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static KeyItem createKeyItem(Item.Properties properties) {
        return new CuriosKeyItem(properties);
    }

    public static Optional<ItemStack> findEquippedKey(Player player) {
        ICuriosItemHandler curiosInventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (curiosInventory == null) return Optional.empty();

        return curiosInventory.getStacksHandler("key").map(slotInventory -> {
            IItemHandler handler = slotInventory.getStacks();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack itemStack = handler.getStackInSlot(i);
                if (!itemStack.isEmpty() && itemStack.getItem() == ModItems.KEY.get()) {
                    return itemStack;
                }
            }
            return ItemStack.EMPTY;
        }).filter(stack -> !stack.isEmpty());
    }

    /** A key that can be worn in the Curios key slot. */
    private static class CuriosKeyItem extends KeyItem implements ICurioItem {

        CuriosKeyItem(Properties properties) {
            super(properties);
        }

        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }
    }
}
