package com.styenvy.egstorage.container;

import com.styenvy.egstorage.blockentity.PandoraChestBlockEntity;
import com.styenvy.egstorage.init.ModBlocks;
import com.styenvy.egstorage.init.ModMenuTypes;
import com.styenvy.egstorage.storage.PandoraChestSavedData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PandoraChestMenu extends AbstractContainerMenu {

    public static final int ROWS_VISIBLE = 6;
    public static final int COLS = 9;
    public static final int CHEST_SLOTS = ROWS_VISIBLE * COLS;
    private static final int MAX_SEARCH_LENGTH = 50;
    private static final int SLOT_SIZE = 18;
    private static final int LEFT_PADDING = 8;
    private static final int CHEST_SLOT_Y = 18;
    private static final int PLAYER_INVENTORY_SLOT_Y = 152;
    private static final int HOTBAR_SLOT_Y = 210;

    private final PandoraChestBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final PandoraChestSavedData.PlayerStorage storage;

    private final List<ItemStack> displayedItems = new ArrayList<>();
    private final int[] displayedIndices = new int[CHEST_SLOTS];
    private final long[] displayedCounts = new long[CHEST_SLOTS];
    private final IntArrayList filteredIndices = new IntArrayList();

    private String searchText = "";
    private int scrollOffset = 0;
    private boolean filterDirty = true;
    private long syncedItemCount = 0;
    private int syncedStackCount = 0;
    private int syncedMaxScroll = 0;

    // === Constructors ===

    /**
     * Client-side constructor (called via network)
     */
    public PandoraChestMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, readBlockEntity(playerInventory, data), PandoraChestSavedData.PlayerStorage.clientOnly());
    }

    /**
     * Server-side constructor
     */
    public PandoraChestMenu(int id, Inventory playerInventory, PandoraChestBlockEntity blockEntity, PandoraChestSavedData.PlayerStorage storage) {
        super(ModMenuTypes.PANDORA_CHEST_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.storage = storage;

        // Slot layout matching PandoraChestScreen.
        // Slot backgrounds are drawn one pixel up/left from these coordinates.

        // Virtual chest slots (6 rows x 9 cols)
        for (int row = 0; row < ROWS_VISIBLE; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                int x = LEFT_PADDING + col * SLOT_SIZE;
                int y = CHEST_SLOT_Y + row * SLOT_SIZE;
                this.addSlot(new VirtualSlot(this, index, x, y));
            }
        }

        // Player main inventory (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = LEFT_PADDING + col * SLOT_SIZE;
                int y = PLAYER_INVENTORY_SLOT_Y + row * SLOT_SIZE;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        // Player hotbar (1 row x 9 cols)
        for (int col = 0; col < 9; col++) {
            int x = LEFT_PADDING + col * SLOT_SIZE;
            int y = HOTBAR_SLOT_Y;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }

        addSyncedLong(this::getServerItemCount, value -> this.syncedItemCount = value, () -> this.syncedItemCount);
        addSyncedInt(storage::getStackCount, value -> this.syncedStackCount = value, () -> this.syncedStackCount);
        addSyncedInt(this::calculateMaxScroll, value -> this.syncedMaxScroll = value, () -> this.syncedMaxScroll);
        addSyncedInt(() -> scrollOffset, value -> this.scrollOffset = value, () -> this.scrollOffset);
        for (int slot = 0; slot < CHEST_SLOTS; slot++) {
            final int displayedSlot = slot;
            addSyncedLong(
                    () -> displayedCounts[displayedSlot],
                    value -> displayedCounts[displayedSlot] = value,
                    () -> displayedCounts[displayedSlot]);
        }

        // Initialize display
        updateDisplayedItems();
    }

    // === Search & Scroll ===

    public void updateSearch(String text) {
        this.searchText = normalizeSearch(text);
        this.scrollOffset = 0;
        this.filterDirty = true;
        updateDisplayedItems();
    }

    public void scroll(int newOffset) {
        int maxScroll = getMaxScroll();
        this.scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
        updateDisplayedItems();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getMaxScroll() {
        if (isClientSideMenu()) {
            return syncedMaxScroll;
        }
        return calculateMaxScroll();
    }

    public long getSyncedItemCount() {
        return isClientSideMenu() ? syncedItemCount : storage.getCurrentItemCount();
    }

    public int getSyncedStackCount() {
        return isClientSideMenu() ? syncedStackCount : storage.getStackCount();
    }

    private int calculateMaxScroll() {
        rebuildFilteredIndicesIfNeeded();
        int totalRows = (filteredIndices.size() + COLS - 1) / COLS; // Ceiling division
        return Math.max(0, totalRows - ROWS_VISIBLE);
    }

    // === Filtering ===

    private void rebuildFilteredIndicesIfNeeded() {
        if (!filterDirty) {
            return;
        }

        filteredIndices.clear();
        List<PandoraChestSavedData.StoredItem> source = storage.getStoredItems();
        for (int i = 0; i < source.size(); i++) {
            if (searchText.isEmpty() || matchesSearch(source.get(i).getPrototype())) {
                filteredIndices.add(i);
            }
        }
        filterDirty = false;
    }

    private boolean matchesSearch(ItemStack stack) {
        String displayName = stack.getDisplayName().getString().toLowerCase(Locale.ROOT);
        String itemId = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return displayName.contains(searchText) || itemId.contains(searchText);
    }

    // === Display Update ===

    private void updateDisplayedItems() {
        displayedItems.clear();
        Arrays.fill(displayedIndices, -1);
        Arrays.fill(displayedCounts, 0);

        rebuildFilteredIndicesIfNeeded();
        List<PandoraChestSavedData.StoredItem> storedItems = storage.getStoredItems();

        int startIndex = scrollOffset * COLS;
        int endIndex = Math.min(startIndex + CHEST_SLOTS, filteredIndices.size());

        for (int visual = 0; visual < endIndex - startIndex; visual++) {
            int beIndex = filteredIndices.get(startIndex + visual);
            ItemStack target = storedItems.get(beIndex).copyForDisplay();

            displayedItems.add(target);
            displayedIndices[visual] = beIndex;
            displayedCounts[visual] = storedItems.get(beIndex).getCount();
        }

        // Update virtual slots
        for (int i = 0; i < CHEST_SLOTS; i++) {
            VirtualSlot slot = (VirtualSlot) getSlot(i);
            if (i < displayedItems.size()) {
                slot.setVisual(displayedItems.get(i), displayedIndices[i]);
            } else {
                slot.setVisual(ItemStack.EMPTY, -1);
            }
        }

        broadcastChanges();
    }

    // === Transfer Handling ===

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        if (player.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        // Chest slots -> Player inventory
        if (slotIndex < CHEST_SLOTS) {
            return quickMoveFromChest(slotIndex);
        }

        // Player inventory/hotbar -> Chest
        return quickMoveToChest(slotIndex);
    }

    private ItemStack quickMoveFromChest(int slotIndex) {
        VirtualSlot vSlot = (VirtualSlot) this.slots.get(slotIndex);
        ItemStack display = vSlot.getItem();
        int beIndex = vSlot.getBeIndex();

        if (display.isEmpty() || beIndex < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = storage.removeItem(beIndex, display.getCount());
        if (removed.isEmpty()) {
            filterDirty = true;
            updateDisplayedItems();
            return ItemStack.EMPTY;
        }

        ItemStack remainder = removed.copy();
        if (!this.moveItemStackTo(remainder, CHEST_SLOTS, this.slots.size(), true)) {
            storage.restoreItem(removed);
            filterDirty = true;
            updateDisplayedItems();
            return ItemStack.EMPTY;
        }

        int movedCount = removed.getCount() - remainder.getCount();
        if (movedCount <= 0) {
            storage.restoreItem(removed);
            filterDirty = true;
            updateDisplayedItems();
            return ItemStack.EMPTY;
        }

        if (!remainder.isEmpty()) {
            storage.restoreItem(remainder);
        }

        filterDirty = true;
        updateDisplayedItems();
        return removed.copyWithCount(movedCount);
    }

    private ItemStack quickMoveToChest(int slotIndex) {
        Slot fromSlot = this.slots.get(slotIndex);
        if (!fromSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = fromSlot.getItem();
        ItemStack original = stack.copy();

        // Insert into the player's saved Pandora storage.
        ItemStack leftover = storage.addItemReturningLeftover(stack);
        int insertedCount = original.getCount() - leftover.getCount();

        if (insertedCount > 0) {
            stack.shrink(insertedCount);
            fromSlot.setChanged();
            filterDirty = true;
            updateDisplayedItems();
            return original;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.PANDORA_CHEST.get());
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < CHEST_SLOTS && handleVirtualSlotClick(slotId, button, clickType, player)) {
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canDragTo(@NotNull Slot slot) {
        return !(slot instanceof VirtualSlot);
    }

    public PandoraChestBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public long getDisplayedCount(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= CHEST_SLOTS) {
            return 0;
        }
        return displayedCounts[slotIndex];
    }

    public void applyClientViewState(String searchText, int requestedScrollOffset) {
        this.searchText = normalizeSearch(searchText);
        this.filterDirty = true;
        int maxScroll = calculateMaxScroll();
        this.scrollOffset = Math.max(0, Math.min(requestedScrollOffset, maxScroll));
        updateDisplayedItems();
    }

    private long getServerItemCount() {
        return storage.getCurrentItemCount();
    }

    private boolean isClientSideMenu() {
        return blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide;
    }

    private static PandoraChestBlockEntity readBlockEntity(Inventory playerInventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof PandoraChestBlockEntity pandoraChest) {
            return pandoraChest;
        }

        throw new IllegalStateException("Pandora's Chest menu opened for missing block entity at " + pos);
    }

    private String normalizeSearch(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.toLowerCase(Locale.ROOT).trim();
        return normalized.length() > MAX_SEARCH_LENGTH ? normalized.substring(0, MAX_SEARCH_LENGTH) : normalized;
    }

    private interface LongSetter {
        void set(long value);
    }

    private interface LongGetter {
        long get();
    }

    private interface IntSetter {
        void set(int value);
    }

    private interface IntGetter {
        int get();
    }

    private void addSyncedInt(IntGetter getter, IntSetter setter, IntGetter currentSyncedValue) {
        for (int word = 0; word < 2; word++) {
            final int shift = word * 16;
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (getter.get() >>> shift) & 0xFFFF;
                }

                @Override
                public void set(int value) {
                    int current = isClientSideMenu() ? currentSyncedValue.get() : getter.get();
                    setter.set((current & ~(0xFFFF << shift)) | ((value & 0xFFFF) << shift));
                }
            });
        }
    }

    private void addSyncedLong(LongGetter getter, LongSetter setter, LongGetter currentSyncedValue) {
        for (int word = 0; word < 4; word++) {
            final int shift = word * 16;
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (int) ((getter.get() >>> shift) & 0xFFFFL);
                }

                @Override
                public void set(int value) {
                    long current = isClientSideMenu() ? currentSyncedValue.get() : getter.get();
                    setter.set((current & ~(0xFFFFL << shift)) | ((long) (value & 0xFFFF) << shift));
                }
            });
        }
    }

    private boolean handleVirtualSlotClick(int slotIndex, int button, ClickType clickType, Player player) {
        return switch (clickType) {
            case PICKUP -> {
                handleVirtualPickup(slotIndex, button, player);
                yield true;
            }
            case QUICK_MOVE -> {
                quickMoveStack(player, slotIndex);
                yield true;
            }
            case SWAP -> {
                handleVirtualSwap(slotIndex, button, player);
                yield true;
            }
            case THROW -> {
                handleVirtualThrow(slotIndex, button, player);
                yield true;
            }
            case CLONE -> {
                handleVirtualClone(slotIndex, player);
                yield true;
            }
            default -> false;
        };
    }

    private void handleVirtualPickup(int slotIndex, int button, Player player) {
        VirtualSlot slot = (VirtualSlot) this.slots.get(slotIndex);
        ItemStack carried = getCarried();

        if (carried.isEmpty()) {
            ItemStack display = slot.getItem();
            if (display.isEmpty()) {
                return;
            }

            int amount = button == 1 ? (display.getCount() + 1) / 2 : display.getCount();
            ItemStack removed = removeFromVirtualSlot(slotIndex, amount);
            if (!removed.isEmpty()) {
                setCarried(removed);
                slot.onTake(player, removed);
            }
            return;
        }

        int amount = button == 1 ? 1 : carried.getCount();
        insertCarriedIntoStorage(carried, amount);
        slot.setChanged();
    }

    private void handleVirtualSwap(int slotIndex, int button, Player player) {
        if ((button < 0 || button >= 9) && button != 40) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack inventoryStack = inventory.getItem(button);
        ItemStack removed = removeFromVirtualSlot(slotIndex, getSlot(slotIndex).getItem().getCount());

        if (!inventoryStack.isEmpty()) {
            ItemStack toStore = inventoryStack.copy();
            int inserted = insertStackIntoStorage(toStore, toStore.getCount());
            if (inserted <= 0 && !removed.isEmpty()) {
                restoreToStorage(removed);
                return;
            }
        }

        inventory.setItem(button, removed);
    }

    private void handleVirtualThrow(int slotIndex, int button, Player player) {
        ItemStack display = getSlot(slotIndex).getItem();
        if (display.isEmpty() || !getCarried().isEmpty()) {
            return;
        }

        int amount = button == 0 ? 1 : display.getCount();
        ItemStack removed = removeFromVirtualSlot(slotIndex, amount);
        if (!removed.isEmpty() && !player.level().isClientSide) {
            player.drop(removed, true);
        }
    }

    private void handleVirtualClone(int slotIndex, Player player) {
        ItemStack display = getSlot(slotIndex).getItem();
        if (player.hasInfiniteMaterials() && getCarried().isEmpty() && !display.isEmpty()) {
            setCarried(display.copyWithCount(display.getMaxStackSize()));
        }
    }

    private int insertCarriedIntoStorage(ItemStack carried, int amount) {
        int inserted = insertStackIntoStorage(carried, amount);
        if (inserted > 0) {
            carried.shrink(inserted);
        }
        return inserted;
    }

    private int insertStackIntoStorage(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }

        int requested = Math.min(amount, stack.getCount());
        if (isClientSideMenu()) {
            return requested;
        }

        ItemStack toInsert = stack.copyWithCount(requested);
        ItemStack leftover = storage.addItemReturningLeftover(toInsert);
        int inserted = requested - leftover.getCount();
        if (inserted > 0) {
            filterDirty = true;
            updateDisplayedItems();
        }
        return inserted;
    }

    private ItemStack removeFromVirtualSlot(int slotIndex, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        VirtualSlot slot = (VirtualSlot) this.slots.get(slotIndex);
        if (isClientSideMenu()) {
            ItemStack removed = slot.remove(amount);
            if (!removed.isEmpty()) {
                displayedCounts[slotIndex] = Math.max(0, displayedCounts[slotIndex] - removed.getCount());
            }
            return removed;
        }

        ItemStack removed = storage.removeItem(slot.getBeIndex(), amount);
        if (!removed.isEmpty()) {
            filterDirty = true;
            updateDisplayedItems();
        }
        return removed;
    }

    private void restoreToStorage(ItemStack stack) {
        if (stack.isEmpty() || isClientSideMenu()) {
            return;
        }

        storage.restoreItem(stack);
        filterDirty = true;
        updateDisplayedItems();
    }

    // === Virtual Slot Implementation ===

    private static class VirtualSlot extends Slot {
        private static final Container EMPTY_CONTAINER = new EmptyContainer();

        private final PandoraChestMenu menu;
        private ItemStack displayStack = ItemStack.EMPTY;
        private int beIndex = -1;

        public VirtualSlot(PandoraChestMenu menu, int index, int x, int y) {
            super(EMPTY_CONTAINER, index, x, y);
            this.menu = menu;
        }

        void setVisual(ItemStack stack, int beIndex) {
            this.displayStack = stack;
            this.beIndex = beIndex;
        }

        int getBeIndex() {
            return beIndex;
        }

        @Override
        public @NotNull ItemStack getItem() {
            return displayStack;
        }

        /**
         * Called when player places items into this slot (click with carried stack)
         */
        @Override
        public void set(@NotNull ItemStack carried) {
            this.displayStack = carried.copy();
            this.beIndex = carried.isEmpty() ? -1 : this.index;
        }

        @Override
        public void setByPlayer(@NotNull ItemStack newStack, @NotNull ItemStack oldStack) {
            set(newStack);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return !stack.isEmpty();
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return beIndex >= 0 && !displayStack.isEmpty();
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
            // Client-side prediction: just take from the visual stack.
            // Server-side: actually remove from the player's saved storage.
            if (menu.blockEntity.getLevel() != null && menu.blockEntity.getLevel().isClientSide) {
                if (displayStack.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                return displayStack.split(amount);
            }

            if (beIndex < 0 || displayStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack removed = menu.storage.removeItem(beIndex, amount);
            if (!removed.isEmpty()) {
                menu.filterDirty = true;
                menu.updateDisplayedItems();
            }
            return removed;
        }

        @Override
        public @NotNull ItemStack safeInsert(@NotNull ItemStack stack, int increment) {
            if (!stack.isEmpty() && increment > 0) {
                int inserted = menu.insertCarriedIntoStorage(stack, increment);
                if (inserted > 0) {
                    setChanged();
                }
            }
            return stack;
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return stack.getMaxStackSize();
        }

        /**
         * Empty container implementation for virtual slots
         */
        private static class EmptyContainer implements Container {
            @Override public int getContainerSize() { return 0; }
            @Override public boolean isEmpty() { return true; }
            @Override public @NotNull ItemStack getItem(int index) { return ItemStack.EMPTY; }
            @Override public @NotNull ItemStack removeItem(int index, int count) { return ItemStack.EMPTY; }
            @Override public @NotNull ItemStack removeItemNoUpdate(int index) { return ItemStack.EMPTY; }
            @Override public void setItem(int index, @NotNull ItemStack stack) {}
            @Override public void setChanged() {}
            @Override public boolean stillValid(@NotNull Player player) { return true; }
            @Override public void clearContent() {}
        }
    }
}
