package net.luckyowlstudios.locksmith.fabric.mixin;

import net.luckyowlstudios.locksmith.event.LockEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Key duplication in the anvil (NeoForge does this with its anvil events): a key plus an iron ingot
 * gives a copy of the key for one level.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void locksmith$duplicateKey(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();
        if (LockEvents.isKeyDuplication(left, right)) {
            menu.getSlot(2).set(left.copy());
            AnvilMenuAccessor accessor = (AnvilMenuAccessor) menu;
            accessor.locksmith$getCost().set(1);
            accessor.locksmith$setRepairItemCountCost(1);
            menu.broadcastChanges();
            ci.cancel();
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void locksmith$onTakeDuplicatedKey(Player player, ItemStack stack, CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        LockEvents.onKeyDuplicated(player, menu.getSlot(0).getItem(), menu.getSlot(1).getItem());
    }
}
