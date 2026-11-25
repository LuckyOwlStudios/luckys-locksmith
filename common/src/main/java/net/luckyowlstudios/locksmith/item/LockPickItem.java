package net.luckyowlstudios.locksmith.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Right-click another player's locked container with this to try the lock picking mini game. The
 * logic lives in {@link net.luckyowlstudios.locksmith.event.LockPicking}.
 */
public class LockPickItem extends Item {

    /** Uses before the pick wears out. Each attempt costs one use, and a failed attempt snaps the pick. */
    public static final int DURABILITY = 8;

    public LockPickItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.locksmith.lock_pick").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.locksmith.lock_pick_xp").withStyle(ChatFormatting.GRAY));
    }
}
