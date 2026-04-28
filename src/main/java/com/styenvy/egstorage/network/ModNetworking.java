package com.styenvy.egstorage.network;

import com.styenvy.egstorage.container.PandoraChestMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PandoraChestViewPayload.TYPE, PandoraChestViewPayload.STREAM_CODEC, ModNetworking::handlePandoraChestView);
    }

    private static void handlePandoraChestView(PandoraChestViewPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu.containerId != payload.containerId()) {
            return;
        }

        if (player.containerMenu instanceof PandoraChestMenu menu) {
            menu.applyClientViewState(payload.searchText(), payload.scrollOffset());
        }
    }
}
