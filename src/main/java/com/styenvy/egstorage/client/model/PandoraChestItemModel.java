package com.styenvy.egstorage.client.model;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.item.PandoraChestItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

@SuppressWarnings("deprecation")
public class PandoraChestItemModel extends GeoModel<PandoraChestItem> {
    @Override
    public ResourceLocation getModelResource(PandoraChestItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "geo/block/pandoras_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PandoraChestItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "textures/block/pandoras_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PandoraChestItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "animations/block/pandoras_chest.animation.json");
    }
}
