package com.styenvy.egstorage.client.renderer.block;

import com.styenvy.egstorage.blockentity.PandoraChestBlockEntity;
import com.styenvy.egstorage.client.model.PandoraChestModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PandoraChestRenderer extends GeoBlockRenderer<PandoraChestBlockEntity> {
    public PandoraChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new PandoraChestModel());
    }

    @Override
    public RenderType getRenderType(PandoraChestBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entitySolid(texture);
    }
}
