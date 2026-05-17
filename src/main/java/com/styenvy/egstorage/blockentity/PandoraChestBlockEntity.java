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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pandora's Chest is only an access point. The actual storage is saved once per
 * player in world saved data, so breaking a block cannot delete the contents.
 *
 * This block entity intentionally does not expose an item handler capability:
 * NeoForge block capabilities have no player context, while this storage is
 * player-scoped.
 */
public class PandoraChestBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    private static final String CONTROLLER = "pandora_controller";
    private static final RawAnimation CLOSED_IDLE = RawAnimation.begin().thenLoop("animation.pandoras_chest.closed_idle");
    private static final RawAnimation OPEN_IDLE = RawAnimation.begin().thenLoop("animation.pandoras_chest.open_idle");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("animation.pandoras_chest.open");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlay("animation.pandoras_chest.close");
    private static final RawAnimation LOOT_BURST = RawAnimation.begin().thenPlay("animation.pandoras_chest.loot_burst");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final List<LegacyStoredItem> legacyBlockItems = new ArrayList<>();
    private final Set<UUID> openViewers = new HashSet<>();
    private boolean open;

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
            onMenuOpened(serverPlayer);
            return new PandoraChestMenu(id, playerInventory, this, storage);
        }

        return new PandoraChestMenu(id, playerInventory, this, PandoraChestSavedData.PlayerStorage.clientOnly());
    }

    public boolean isOpen() {
        return open;
    }

    public void onMenuOpened(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (openViewers.add(player.getUUID()) && openViewers.size() == 1) {
            setOpen(true);
        }
    }

    public void onMenuClosed(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }

        openViewers.remove(player.getUUID());
        if (openViewers.isEmpty()) {
            setOpen(false);
        }
    }

    public void triggerLootBurst() {
        if (level != null && !level.isClientSide) {
            triggerAnim(CONTROLLER, "loot_burst");
            syncAnimationState();
        }
    }

    private void setOpen(boolean open) {
        if (this.open == open) {
            return;
        }

        this.open = open;
        if (level != null && !level.isClientSide) {
            triggerAnim(CONTROLLER, open ? "open" : "close");
            syncAnimationState();
        }
    }

    private void syncAnimationState() {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> {
            state.setAndContinue(open ? OPEN_IDLE : CLOSED_IDLE);
            return PlayState.CONTINUE;
        })
                .triggerableAnim("open", OPEN)
                .triggerableAnim("close", CLOSE)
                .triggerableAnim("loot_burst", LOOT_BURST));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Open", open);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.handleUpdateTag(tag, registries);
        open = tag.getBoolean("Open");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private record LegacyStoredItem(ItemStack stack, long count) {
    }
}
