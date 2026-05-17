package com.styenvy.egstorage.client.renderer.item;

import com.styenvy.egstorage.client.model.PandoraChestItemModel;
import com.styenvy.egstorage.item.PandoraChestItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PandoraChestItemRenderer extends GeoItemRenderer<PandoraChestItem> {
    public PandoraChestItemRenderer() {
        super(new PandoraChestItemModel());
    }

    @Override
    public RenderType getRenderType(PandoraChestItem animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entitySolid(texture);
    }
}
