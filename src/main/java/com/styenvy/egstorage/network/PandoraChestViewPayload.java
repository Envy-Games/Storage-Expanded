package com.styenvy.egstorage.network;

import com.styenvy.egstorage.EGStorageMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PandoraChestViewPayload(int containerId, String searchText, int scrollOffset) implements CustomPacketPayload {
    public static final Type<PandoraChestViewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "pandora_chest_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PandoraChestViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PandoraChestViewPayload::containerId,
            ByteBufCodecs.STRING_UTF8,
            PandoraChestViewPayload::searchText,
            ByteBufCodecs.VAR_INT,
            PandoraChestViewPayload::scrollOffset,
            PandoraChestViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
