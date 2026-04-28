package com.styenvy.egstorage.storage;

import com.styenvy.egstorage.EGStorageMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PandoraChestSavedData extends SavedData {
    private static final String DATA_PREFIX = EGStorageMod.MODID + "_pandora_chest_";

    private PlayerStorage storage = new PlayerStorage(this::setDirty);

    public static PlayerStorage getStorage(MinecraftServer server, UUID owner) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(PandoraChestSavedData::new, PandoraChestSavedData::load),
                        DATA_PREFIX + owner)
                .storage;
    }

    private static PandoraChestSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PandoraChestSavedData data = new PandoraChestSavedData();
        data.storage = PlayerStorage.load(tag, registries, data::setDirty);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return storage.save(tag, registries);
    }

    public static class PlayerStorage {
        private final List<StoredItem> storedItems = new ArrayList<>();
        private final Runnable dirtyMarker;
        private long totalItemsStored = 0;
        private long currentItemCount = 0;

        private PlayerStorage(Runnable dirtyMarker) {
            this.dirtyMarker = dirtyMarker;
        }

        public static PlayerStorage clientOnly() {
            return new PlayerStorage(() -> {
            });
        }

        private static PlayerStorage load(CompoundTag tag, HolderLookup.Provider registries, Runnable dirtyMarker) {
            PlayerStorage storage = new PlayerStorage(dirtyMarker);
            ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);

            long computedCurrentCount = 0;
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag itemTag = itemList.getCompound(i);
                ItemStack stack = ItemStack.parseOptional(registries, itemTag.getCompound("Item"));
                if (!stack.isEmpty()) {
                    long count = itemTag.contains("Count", Tag.TAG_LONG) ? itemTag.getLong("Count") : stack.getCount();
                    if (count > 0) {
                        storage.storedItems.add(new StoredItem(stack, count));
                        computedCurrentCount = safeAdd(computedCurrentCount, count);
                    }
                }
            }

            storage.currentItemCount = computedCurrentCount;
            storage.totalItemsStored = Math.max(tag.getLong("TotalStored"), computedCurrentCount);
            return storage;
        }

        private CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag itemList = new ListTag();

            for (StoredItem stored : storedItems) {
                if (!stored.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.put("Item", stored.getPrototype().save(registries));
                    itemTag.putLong("Count", stored.getCount());
                    itemList.add(itemTag);
                }
            }

            tag.put("Items", itemList);
            tag.putLong("TotalStored", totalItemsStored);
            tag.putLong("CurrentCount", currentItemCount);
            return tag;
        }

        public List<StoredItem> getStoredItems() {
            return Collections.unmodifiableList(storedItems);
        }

        public ItemStack addItemReturningLeftover(@NotNull ItemStack input) {
            insertCompressed(input, input.getCount(), true);
            return ItemStack.EMPTY;
        }

        public void restoreItem(@NotNull ItemStack stack) {
            insertCompressed(stack, stack.getCount(), false);
        }

        public void restoreItemCount(@NotNull ItemStack stack, long count) {
            insertCompressed(stack, count, false);
        }

        private void insertCompressed(@NotNull ItemStack input, long amount, boolean countAsNewStorage) {
            if (input.isEmpty() || amount <= 0) {
                return;
            }

            ItemStack toInsert = input.copy();
            toInsert.setCount(1);
            for (StoredItem existing : storedItems) {
                if (existing.isSameItem(toInsert)) {
                    existing.grow(amount);
                    currentItemCount = safeAdd(currentItemCount, amount);
                    if (countAsNewStorage) {
                        totalItemsStored = safeAdd(totalItemsStored, amount);
                    }
                    markDirty();
                    return;
                }
            }

            storedItems.add(new StoredItem(toInsert, amount));
            currentItemCount = safeAdd(currentItemCount, amount);
            if (countAsNewStorage) {
                totalItemsStored = safeAdd(totalItemsStored, amount);
            }
            markDirty();
        }

        public ItemStack removeItem(int index, int count) {
            if (index < 0 || index >= storedItems.size() || count <= 0) {
                return ItemStack.EMPTY;
            }

            StoredItem stored = storedItems.get(index);
            ItemStack removed = stored.remove(count);

            if (stored.isEmpty()) {
                storedItems.remove(index);
            }

            if (!removed.isEmpty()) {
                currentItemCount = Math.max(0, currentItemCount - removed.getCount());
                markDirty();
            }

            return removed;
        }

        public long getCurrentItemCount() {
            return currentItemCount;
        }

        public int getStackCount() {
            return storedItems.size();
        }

        public long getTotalItemsStored() {
            return totalItemsStored;
        }

        private void markDirty() {
            dirtyMarker.run();
        }
    }

    public static class StoredItem {
        private final ItemStack prototype;
        private long count;

        private StoredItem(ItemStack stack, long count) {
            this.prototype = stack.copy();
            this.prototype.setCount(1);
            this.count = Math.max(0, count);
        }

        public ItemStack getPrototype() {
            return prototype;
        }

        public long getCount() {
            return count;
        }

        public boolean isSameItem(ItemStack stack) {
            return ItemStack.isSameItemSameComponents(prototype, stack);
        }

        public boolean isEmpty() {
            return prototype.isEmpty() || count <= 0;
        }

        private void grow(long amount) {
            if (amount > 0) {
                count = safeAdd(count, amount);
            }
        }

        private ItemStack remove(int amount) {
            int removedCount = (int) Math.min(Math.max(amount, 0), Math.min(count, prototype.getMaxStackSize()));
            if (removedCount <= 0) {
                return ItemStack.EMPTY;
            }

            count -= removedCount;
            return copyWithCount(removedCount);
        }

        public ItemStack copyForDisplay() {
            return copyWithCount((int) Math.min(count, prototype.getMaxStackSize()));
        }

        private ItemStack copyWithCount(int amount) {
            ItemStack copy = prototype.copy();
            copy.setCount(amount);
            return copy;
        }
    }

    private static long safeAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
