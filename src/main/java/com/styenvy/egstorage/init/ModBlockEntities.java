package com.styenvy.egstorage.init;

import com.styenvy.egstorage.EGStorageMod;
import com.styenvy.egstorage.blockentity.PandoraChestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EGStorageMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PandoraChestBlockEntity>> PANDORA_CHEST_BE =
            BLOCK_ENTITIES.register("pandora_chest", () ->
                    BlockEntityType.Builder.of(PandoraChestBlockEntity::new, ModBlocks.PANDORA_CHEST.get()).build(null));
}