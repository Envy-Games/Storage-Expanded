package com.styenvy.egstorage.init;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.block.PandoraChestBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(EGStorageMod.MODID);

    public static final DeferredBlock<PandoraChestBlock> PANDORA_CHEST =
            BLOCKS.register("pandora_chest", () -> new PandoraChestBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(50.0F, 1200.0F) // Strong like obsidian
                            .lightLevel(state -> 7) // Mystical glow
                            .requiresCorrectToolForDrops()));
}