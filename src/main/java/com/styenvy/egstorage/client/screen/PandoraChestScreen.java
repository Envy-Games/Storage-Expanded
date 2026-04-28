package com.styenvy.egstorage.client.screen;

import com.styenvy.egstorage.container.PandoraChestMenu;
import com.styenvy.egstorage.network.PandoraChestViewPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pandora Chest Screen - renders using only vanilla GUI sprites and graphics primitives.
 * No custom texture files required!
 */
public class PandoraChestScreen extends AbstractContainerScreen<PandoraChestMenu> {

    // Vanilla sprite location (1.20.2+ sprite system)
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");

    // Container colors (matching vanilla container style)
    private static final int BG_COLOR = 0xFFC6C6C6;           // Main background
    private static final int BORDER_LIGHT = 0xFFFFFFFF;        // Top/left border highlight
    private static final int BORDER_DARK = 0xFF555555;         // Bottom/right border shadow
    private static final int BORDER_DARKER = 0xFF373737;       // Outer shadow
    private static final int SLOT_BG = 0xFF8B8B8B;             // Slot background
    private static final int SLOT_BORDER_DARK = 0xFF373737;    // Slot inner shadow
    private static final int SLOT_BORDER_LIGHT = 0xFFFFFFFF;   // Slot inner highlight
    private static final int TITLE_COLOR = 0x9932CC;           // Mystical purple for title
    private static final int TEXT_COLOR = 0x404040;            // Standard label color
    private static final int SCROLLBAR_BG = 0xFF000000;        // Scrollbar track

    // Layout constants
    private static final int CHEST_ROWS = 6;
    private static final int CHEST_COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_HEIGHT = 108;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int LEFT_PADDING = 8;
    private static final int CHEST_SLOT_X = LEFT_PADDING;
    private static final int CHEST_SLOT_Y = 18;
    private static final int SCROLLBAR_X = 175;
    private static final int SCROLLBAR_Y = CHEST_SLOT_Y;
    private static final int SEPARATOR_Y = 126;
    private static final int STATUS_Y = 129;
    private static final int PLAYER_INVENTORY_LABEL_Y = 140;
    private static final int PLAYER_INVENTORY_SLOT_Y = 152;
    private static final int HOTBAR_SLOT_Y = 210;
    private static final int SEARCH_X = 96;
    private static final int SEARCH_Y = 5;
    private static final int SEARCH_WIDTH = 75;
    private static final int SEARCH_HEIGHT = 12;

    private float scrollOffset;
    private boolean isScrolling;
    private EditBox searchBox;

    public PandoraChestScreen(PandoraChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Extended width to accommodate scrollbar
        this.imageWidth = 176 + SCROLLBAR_WIDTH + 4;
        this.imageHeight = 234;
        this.inventoryLabelX = LEFT_PADDING;
        this.inventoryLabelY = PLAYER_INVENTORY_LABEL_Y;
        this.titleLabelX = LEFT_PADDING;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        // Create search box (vanilla EditBox handles its own rendering)
        int searchX = this.leftPos + SEARCH_X;
        int searchY = this.topPos + SEARCH_Y;
        this.searchBox = new EditBox(this.font, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, Component.literal("Search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setHint(Component.literal("Search...").withStyle(s -> s.withColor(0x888888)));
        this.searchBox.setResponder(text -> {
            this.scrollOffset = 0;
            sendViewState();
        });
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isScrolling) {
            updateScrollbar();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // === Main container panel ===
        renderPanel(graphics, x, y, imageWidth, imageHeight);

        // === Chest slots area (6 rows x 9 cols) ===
        int slotsX = x + CHEST_SLOT_X - 1;
        int slotsY = y + CHEST_SLOT_Y - 1;
        renderSlotGrid(graphics, slotsX, slotsY, CHEST_COLS, CHEST_ROWS);

        // === Scrollbar ===
        renderScrollbar(graphics, x + SCROLLBAR_X, y + SCROLLBAR_Y);

        // === Player inventory slots (3 rows x 9 cols) ===
        int invY = y + PLAYER_INVENTORY_SLOT_Y - 1;
        renderSlotGrid(graphics, slotsX, invY, 9, 3);

        // === Hotbar slots (1 row x 9 cols) ===
        int hotbarY = y + HOTBAR_SLOT_Y - 1;
        renderSlotGrid(graphics, slotsX, hotbarY, 9, 1);

        // === Separator line between chest and inventory ===
        graphics.fill(x + CHEST_SLOT_X - 1, y + SEPARATOR_Y, x + CHEST_SLOT_X - 1 + CHEST_COLS * SLOT_SIZE, y + SEPARATOR_Y + 1, BORDER_DARK);
        graphics.fill(x + CHEST_SLOT_X - 1, y + SEPARATOR_Y + 1, x + CHEST_SLOT_X - 1 + CHEST_COLS * SLOT_SIZE, y + SEPARATOR_Y + 2, BORDER_LIGHT);

        // === Item count display ===
        long totalItems = menu.getSyncedItemCount();
        int uniqueStacks = menu.getSyncedStackCount();
        if (totalItems > 0) {
            String countText = formatItemCount(totalItems) + " items (" + uniqueStacks + " stacks)";
            graphics.drawString(this.font, countText, x + LEFT_PADDING, y + STATUS_Y, TEXT_COLOR, false);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Render title with mystical purple color
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, false);
        // Render "Inventory" label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_COLOR, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && this.hoveredSlot.index < PandoraChestMenu.CHEST_SLOTS
                && this.hoveredSlot.hasItem()) {
            ItemStack itemStack = this.hoveredSlot.getItem();
            List<Component> tooltip = new ArrayList<>(this.getTooltipFromContainerItem(itemStack));
            long storedCount = this.menu.getDisplayedCount(this.hoveredSlot.index);
            if (storedCount > 0) {
                tooltip.add(Component.literal("Stored: " + NumberFormat.getIntegerInstance(Locale.US).format(storedCount)));
            }
            graphics.renderTooltip(this.font, tooltip, itemStack.getTooltipImage(), itemStack, x, y);
            return;
        }

        super.renderTooltip(graphics, x, y);
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack itemStack, Slot slot, @Nullable String countString) {
        if (slot.index < PandoraChestMenu.CHEST_SLOTS) {
            long storedCount = menu.getDisplayedCount(slot.index);
            super.renderSlotContents(graphics, itemStack, slot, storedCount > 1 ? formatSlotCount(storedCount) : null);
            return;
        }

        super.renderSlotContents(graphics, itemStack, slot, countString);
    }

    /**
     * Renders a panel with 3D beveled edges (vanilla container style)
     */
    private void renderPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Outer shadow
        graphics.fill(x + 1, y + height, x + width + 1, y + height + 1, BORDER_DARKER);
        graphics.fill(x + width, y + 1, x + width + 1, y + height + 1, BORDER_DARKER);

        // Main background
        graphics.fill(x, y, x + width, y + height, BG_COLOR);

        // Top and left highlight
        graphics.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        graphics.fill(x, y, x + 1, y + height, BORDER_LIGHT);

        // Bottom and right shadow
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
    }

    /**
     * Renders a grid of slot backgrounds with 3D inset effect
     */
    private void renderSlotGrid(GuiGraphics graphics, int startX, int startY, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotX = startX + col * SLOT_SIZE;
                int slotY = startY + row * SLOT_SIZE;
                renderSlot(graphics, slotX, slotY);
            }
        }
    }

    /**
     * Renders a single slot with 3D inset borders
     */
    private void renderSlot(GuiGraphics graphics, int x, int y) {
        // Slot background
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);

        // Inner shadow (top and left)
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + 1, SLOT_BORDER_DARK);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE - 1, SLOT_BORDER_DARK);

        // Inner highlight (bottom and right)
        graphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER_LIGHT);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER_LIGHT);
    }

    /**
     * Renders the scrollbar using vanilla sprites
     */
    private void renderScrollbar(GuiGraphics graphics, int x, int y) {
        // Scrollbar track background
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + SCROLLBAR_HEIGHT, SCROLLBAR_BG);
        graphics.fill(x + 1, y + 1, x + SCROLLBAR_WIDTH - 1, y + SCROLLBAR_HEIGHT - 1, SLOT_BG);

        // Scrollbar thumb position
        int thumbY = y + 1 + (int) ((SCROLLBAR_HEIGHT - SCROLLER_HEIGHT - 2) * scrollOffset);

        if (canScroll()) {
            // Active scrollbar - use vanilla sprite
            graphics.blitSprite(SCROLLER_SPRITE, x + 1, thumbY, SCROLLBAR_WIDTH - 2, SCROLLER_HEIGHT);
        } else {
            // Disabled state - draw a grayed out thumb manually
            int thumbX = x + 1;
            int thumbW = SCROLLBAR_WIDTH - 2;
            // Draw disabled thumb with muted colors
            graphics.fill(thumbX, thumbY, thumbX + thumbW, thumbY + SCROLLER_HEIGHT, 0xFF606060);
            // Simple 3D effect
            graphics.fill(thumbX, thumbY, thumbX + thumbW - 1, thumbY + 1, 0xFF808080);
            graphics.fill(thumbX, thumbY, thumbX + 1, thumbY + SCROLLER_HEIGHT - 1, 0xFF808080);
            graphics.fill(thumbX + thumbW - 1, thumbY, thumbX + thumbW, thumbY + SCROLLER_HEIGHT, 0xFF404040);
            graphics.fill(thumbX, thumbY + SCROLLER_HEIGHT - 1, thumbX + thumbW, thumbY + SCROLLER_HEIGHT, 0xFF404040);
        }
    }

    /**
     * Formats large item counts (e.g., 1.5M instead of 1500000)
     */
    private String formatItemCount(long count) {
        if (count >= 1_000_000_000) {
            return String.format("%.1fB", count / 1_000_000_000.0);
        } else if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    private String formatSlotCount(long count) {
        if (count >= 1_000_000_000_000L) {
            return String.format("%.1fT", count / 1_000_000_000_000.0);
        } else if (count >= 1_000_000_000) {
            return String.format("%.1fB", count / 1_000_000_000.0);
        } else if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 10_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }

    // === Input Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int scrollbarX = this.leftPos + SCROLLBAR_X;
            int scrollbarY = this.topPos + SCROLLBAR_Y;

            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
                    mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT) {
                this.isScrolling = canScroll();
                if (this.isScrolling) {
                    updateScrollFromMouse(mouseY);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling && canScroll()) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (canScroll()) {
            int maxScroll = menu.getMaxScroll();
            float scrollStep = 1.0F / Math.max(1, maxScroll);

            this.scrollOffset = Mth.clamp(this.scrollOffset - (float) scrollY * scrollStep, 0.0F, 1.0F);
            sendViewState();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let search box capture input when focused
        if (this.searchBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void updateScrollFromMouse(double mouseY) {
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        float scrollableHeight = SCROLLBAR_HEIGHT - SCROLLER_HEIGHT - 2;

        float newScroll = ((float) mouseY - scrollbarY - SCROLLER_HEIGHT / 2.0F) / scrollableHeight;
        this.scrollOffset = Mth.clamp(newScroll, 0.0F, 1.0F);
        sendViewState();
    }

    private boolean canScroll() {
        return menu.getMaxScroll() > 0;
    }

    private void updateScrollbar() {
        if (!canScroll()) {
            this.scrollOffset = 0;
        } else {
            int maxScroll = menu.getMaxScroll();
            if (maxScroll > 0) {
                this.scrollOffset = (float) menu.getScrollOffset() / maxScroll;
            }
        }
    }

    private void sendViewState() {
        String searchText = this.searchBox == null ? "" : this.searchBox.getValue();
        int maxScroll = menu.getMaxScroll();
        int requestedScroll = maxScroll <= 0 ? 0 : Mth.clamp((int) (scrollOffset * maxScroll), 0, maxScroll);
        PacketDistributor.sendToServer(new PandoraChestViewPayload(menu.containerId, searchText, requestedScroll));
    }
}
