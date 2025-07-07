package net.luckyowlstudios.locksmith.block.chest.gold;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.block.chest.DyeableChestRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

public class GoldenChestRenderer extends DyeableChestRenderer {

    protected final ModelPart chest;
    protected final ModelPart left;
    protected final ModelPart right;

    public GoldenChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        chest = context.bakeLayer(ModelLayers.CHEST);
        left = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        right = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
    }

    @Override
    protected DyedItemColor defaultColor() {
        return new DyedItemColor(DyeColor.RED.getTextureDiffuseColor(), true);
    }

    @Override
    protected Material getMaterial(ChestBlockEntity blockEntity, ChestType chestType) {
        return switch (chestType) {
            case LEFT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden_left"));
            case RIGHT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden_right"));
            case SINGLE -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/golden"));
        };
    }
}
