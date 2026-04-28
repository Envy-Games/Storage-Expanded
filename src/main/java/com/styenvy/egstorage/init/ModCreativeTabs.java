package com.styenvy.egstorage.init;

import com.styenvy.egstorage.EGStorageMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EGStorageMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EGSTORAGE_TAB =
            CREATIVE_MODE_TABS.register("egstorage_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.PANDORA_CHEST_ITEM.get()))
                            .title(Component.translatable("creativetab.egstorage.mystical_storage"))
                            .displayItems((params, output) -> {
                                output.accept(ModItems.PANDORA_CHEST_ITEM.get());
                            })
                            .build());
}