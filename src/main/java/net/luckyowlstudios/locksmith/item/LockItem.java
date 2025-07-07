package net.luckyowlstudios.locksmith.item;

import net.luckyowlstudios.locksmith.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class LockItem extends Item {

    public LockItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (stack == ModItems.GOLDEN_LOCK.toStack()) {
            tooltipComponents.add(Component.translatable("tooltip.locksmith.golden_lock").withStyle(ChatFormatting.GRAY));
        }
        if (stack == ModItems.TRIAL_LOCK.toStack()) {
            tooltipComponents.add(Component.translatable("tooltip.locksmith.golden_lock").withStyle(ChatFormatting.GRAY));
        }
    }
}
