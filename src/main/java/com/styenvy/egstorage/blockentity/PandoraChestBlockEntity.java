package com.styenvy.egstorage.blockentity;

import com.styenvy.egstorage.container.PandoraChestMenu;
import com.styenvy.egstorage.init.ModBlockEntities;
import com.styenvy.egstorage.storage.PandoraChestSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pandora's Chest is only an access point. The actual storage is saved once per
 * player in world saved data, so breaking a block cannot delete the contents.
 */
public class PandoraChestBlockEntity extends BlockEntity implements MenuProvider {
    private final List<LegacyStoredItem> legacyBlockItems = new ArrayList<>();

    public PandoraChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PANDORA_CHEST_BE.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.egstorage.pandora_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PandoraChestSavedData.PlayerStorage storage = PandoraChestSavedData.getStorage(serverPlayer.getServer(), serverPlayer.getUUID());
            migrateLegacyBlockItems(storage);
            return new PandoraChestMenu(id, playerInventory, this, storage);
        }

        return new PandoraChestMenu(id, playerInventory, this, PandoraChestSavedData.PlayerStorage.clientOnly());
    }

    private void migrateLegacyBlockItems(PandoraChestSavedData.PlayerStorage storage) {
        if (legacyBlockItems.isEmpty()) {
            return;
        }

        for (LegacyStoredItem stored : legacyBlockItems) {
            storage.restoreItemCount(stored.stack(), stored.count());
        }
        legacyBlockItems.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!legacyBlockItems.isEmpty()) {
            ListTag itemList = new ListTag();
            for (LegacyStoredItem stored : legacyBlockItems) {
                if (!stored.stack().isEmpty() && stored.count() > 0) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.put("Item", stored.stack().save(registries));
                    itemTag.putLong("Count", stored.count());
                    itemList.add(itemTag);
                }
            }
            tag.put("Items", itemList);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        legacyBlockItems.clear();
        ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, itemTag.getCompound("Item"));
            if (!stack.isEmpty()) {
                long count = itemTag.contains("Count", Tag.TAG_LONG) ? itemTag.getLong("Count") : stack.getCount();
                legacyBlockItems.add(new LegacyStoredItem(stack, count));
            }
        }
    }

    private record LegacyStoredItem(ItemStack stack, long count) {
    }
}
