package net.luckyowlstudios.locksmith.overrides;

import net.luckyowlstudios.locksmith.Locksmith;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class LockModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Locksmith.id("lock"), "main");

    public static LayerDefinition createLock() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition lock = partdefinition.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 9).addBox(-2.5F, -6.0F, -8.0F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(1, 6).addBox(-2.0F, -9.0F, -7.5F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 14.0F, 14.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
