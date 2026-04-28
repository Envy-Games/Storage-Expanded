package com.styenvy.egstorage.client;

import com.mojang.logging.LogUtils;
import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.client.screen.PandoraChestScreen;
import com.styenvy.egstorage.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = EGStorageMod.MODID, value = Dist.CLIENT)
public final class EGStorageClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private EGStorageClient() {
    }

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("EGStorage Client Setup - Preparing mystical interfaces");
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.PANDORA_CHEST_MENU.get(), PandoraChestScreen::new);
    }
}
