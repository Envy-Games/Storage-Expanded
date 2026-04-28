package com.styenvy.egstorage.init;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.container.PandoraChestMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, EGStorageMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PandoraChestMenu>> PANDORA_CHEST_MENU =
            MENU_TYPES.register("pandora_chest", () ->
                    IMenuTypeExtension.create(PandoraChestMenu::new));
}