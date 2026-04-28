package com.styenvy.egstorage.init;

import com.styenvy.egstorage.EGStorageMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(EGStorageMod.MODID);

    public static final DeferredItem<BlockItem> PANDORA_CHEST_ITEM =
            ITEMS.register("pandora_chest", () -> new BlockItem(
                    ModBlocks.PANDORA_CHEST.get(), new Item.Properties()
                    .rarity(Rarity.EPIC) // Purple text
                    .fireResistant())); // Can't be destroyed by lava
}