package net.luckyowlstudios.locksmith.neoforge.compat;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesAPI;
import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Everything that touches the Accessories API. Only loaded when Accessories is installed.
 */
public final class AccessoriesCompat {

    private AccessoriesCompat() {
    }

    /** Lets the key be worn in the "key" slot (see data/locksmith/accessories). */
    public static void registerKey(Item key) {
        AccessoriesAPI.registerAccessory(key, new Accessory() {
        });
    }

    public static Optional<ItemStack> findEquippedKey(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) return Optional.empty();

        return capability.getEquipped(stack -> stack.getItem() == ModItems.KEY.get()).stream()
                .map(reference -> reference.stack())
                .filter(stack -> !stack.isEmpty())
                .findFirst();
    }
}
