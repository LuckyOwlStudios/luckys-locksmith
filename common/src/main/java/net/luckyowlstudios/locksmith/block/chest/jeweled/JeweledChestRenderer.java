package net.luckyowlstudios.locksmith.block.chest.jeweled;

import net.luckyowlstudios.locksmith.Locksmith;
import net.luckyowlstudios.locksmith.block.chest.DyeableChestRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

public class JeweledChestRenderer extends DyeableChestRenderer {

    protected final ModelPart chest;
    protected final ModelPart left;
    protected final ModelPart right;

    public JeweledChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        chest = context.bakeLayer(ModelLayers.CHEST);
        left = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        right = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
    }

    @Override
    protected String overlayTexture(ChestType chestType, boolean dyed) {
        // Always fabric, purple until dyed
        return "entity/chest/fabric" + sideSuffix(chestType);
    }

    @Override
    protected int undyedColor() {
        return DyeColor.PURPLE.getTextureDiffuseColor();
    }

    @Override
    protected Material getMaterial(ChestBlockEntity blockEntity, ChestType chestType) {
        return switch (chestType) {
            case LEFT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/jeweled_left"));
            case RIGHT -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/jeweled_right"));
            case SINGLE -> new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(Locksmith.MOD_ID, "entity/chest/jeweled"));
        };
    }
}
