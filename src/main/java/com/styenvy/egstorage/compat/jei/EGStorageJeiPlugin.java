package com.styenvy.egstorage.compat.jei;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class EGStorageJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(EGStorageMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(
                ModItems.PANDORA_CHEST_ITEM.get(),
                Component.translatable("jei.egstorage.pandora_chest.info"));
    }
}
