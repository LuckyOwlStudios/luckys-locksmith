package net.luckyowlstudios.locksmith.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class KeyItem extends Item implements ICurioItem {

    public KeyItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Generate a random lock code if it doesn't exist
        if (!stack.getComponents().has(DataComponents.LOCK)) {
            LockCode lockCode = generateRandomCode();
            stack.set(DataComponents.LOCK, lockCode);
        }
    }

    private static LockCode generateRandomCode() {
        // Generate a random 5-digit code or UUID string
        String code = String.valueOf((int)(Math.random() * 90000 + 10000));
        return new LockCode(code);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        LockCode lockCode = stack.get(DataComponents.LOCK);

        if (Screen.hasShiftDown()) {
            if (lockCode != null && !lockCode.key().isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.locksmith.code", lockCode.key()).withStyle(ChatFormatting.GRAY));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.locksmith.no_code").withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.locksmith.reveal_code", Component.keybind(Minecraft.getInstance().options.keyShift.getName())).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
