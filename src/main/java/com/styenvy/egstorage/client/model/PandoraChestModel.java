package com.styenvy.egstorage.client.model;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.blockentity.PandoraChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class PandoraChestModel extends GeoModel<PandoraChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(PandoraChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "geo/block/pandoras_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PandoraChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "textures/block/pandoras_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PandoraChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "animations/block/pandoras_chest.animation.json");
    }
}
