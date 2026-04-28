package com.styenvy.egstorage;

import com.mojang.logging.LogUtils;
import com.styenvy.egstorage.init.ModBlockEntities;
import com.styenvy.egstorage.init.ModBlocks;
import com.styenvy.egstorage.init.ModCreativeTabs;
import com.styenvy.egstorage.init.ModItems;
import com.styenvy.egstorage.init.ModMenuTypes;
import com.styenvy.egstorage.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EGStorageMod.MODID)
public class EGStorageMod {
    public static final String MODID = "egstorage";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EGStorageMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing EGStorage - Mystical Storage Solutions!");

        // Register in correct order - Items must be registered before Creative Tabs
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);
    }
}
