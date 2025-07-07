package net.luckyowlstudios.locksmith.block.chest.iron_trapped;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.block.chest.DyeableChestRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

public class IronTrappedChestRenderer extends DyeableChestRenderer {

    public IronTrappedChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected DyedItemColor defaultColor() {
        return new DyedItemColor(DyeColor.GRAY.getTextureDiffuseColor(), true);
    }

    @Override
    protected Material getMaterial(ChestBlockEntity blockEntity, ChestType chestType) {
        return switch (chestType) {
            case LEFT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron_trapped_left"));
            case RIGHT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron_trapped_right"));
            case SINGLE -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/iron_trapped"));
        };
    }
}
