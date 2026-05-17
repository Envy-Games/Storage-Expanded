package com.styenvy.egstorage.client.screen;

import com.styenvy.egstorage.PandoraChestConstants;
import com.styenvy.egstorage.container.PandoraChestMenu;
import com.styenvy.egstorage.network.PandoraChestViewPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
 * Pandora Chest Screen - rendered with GUI primitives so the eldritch style
 * does not require external texture assets.
 */
public class PandoraChestScreen extends AbstractContainerScreen<PandoraChestMenu> {

    private static final int SHADOW = 0x99000000;
    private static final int OUTER_EDGE = 0xFF05030A;
    private static final int PANEL_EDGE = 0xFF141020;
    private static final int PANEL_BG = 0xFF1A1322;
    private static final int PANEL_INNER = 0xFF221A2D;
    private static final int SECTION_BG = 0xFF100D17;
    private static final int SECTION_EDGE = 0xFF39294C;
    private static final int SLOT_BG = 0xFF171820;
    private static final int SLOT_INNER = 0xFF202331;
    private static final int SLOT_HIGHLIGHT = 0xFF5E497A;
    private static final int SLOT_SHADOW = 0xFF090810;
    private static final int TEXT_PRIMARY = 0xFFEDE7FF;
    private static final int TEXT_MUTED = 0xFF9E94AD;
    private static final int VOID_PURPLE = 0xFF8D59D6;
    private static final int ELDRITCH_TEAL = 0xFF40D9C8;
    private static final int RITUAL_GOLD = 0xFFCB8E4A;
    private static final int SEARCH_BG = 0xFF0B0A10;
    private static final int SCROLLBAR_BG = 0xFF08070C;

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
        this.searchBox.setMaxLength(PandoraChestConstants.MAX_SEARCH_LENGTH);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(TEXT_PRIMARY);
        this.searchBox.setHint(Component.literal("Seek...").withStyle(s -> s.withColor(TEXT_MUTED)));
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
        float pulse = getPulse(partialTick);

        renderPanel(graphics, x, y, imageWidth, imageHeight, pulse);

        int slotsX = x + CHEST_SLOT_X - 1;
        int slotsY = y + CHEST_SLOT_Y - 1;
        renderSectionFrame(graphics, slotsX - 5, slotsY - 5, CHEST_COLS * SLOT_SIZE + 10, CHEST_ROWS * SLOT_SIZE + 10, pulse);
        renderSlotGrid(graphics, slotsX, slotsY, CHEST_COLS, CHEST_ROWS);

        renderScrollbar(graphics, x + SCROLLBAR_X, y + SCROLLBAR_Y, pulse);

        int invY = y + PLAYER_INVENTORY_SLOT_Y - 1;
        int hotbarY = y + HOTBAR_SLOT_Y - 1;
        renderSectionFrame(graphics, slotsX - 5, invY - 5, 9 * SLOT_SIZE + 10, hotbarY - invY + SLOT_SIZE + 10, pulse * 0.65F);
        renderSlotGrid(graphics, slotsX, invY, 9, 3);
        renderSlotGrid(graphics, slotsX, hotbarY, 9, 1);

        renderRitualDivider(graphics, x + CHEST_SLOT_X - 1, y + SEPARATOR_Y, CHEST_COLS * SLOT_SIZE, pulse);

        String countText = formatItemCount(menu.getSyncedItemCount()) + " Items Stored";
        graphics.drawString(this.font, countText, x + LEFT_PADDING, y + STATUS_Y, TEXT_MUTED, false);
        renderSearchFrame(graphics, x + SEARCH_X - 2, y + SEARCH_Y - 2, SEARCH_WIDTH + 4, SEARCH_HEIGHT + 2, pulse);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX + 1, this.titleLabelY + 1, OUTER_EDGE, false);
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_PRIMARY, false);

        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_MUTED, false);
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

    private void renderPanel(GuiGraphics graphics, int x, int y, int width, int height, float pulse) {
        graphics.fill(x + 4, y + 5, x + width + 5, y + height + 6, SHADOW);
        graphics.fill(x, y, x + width, y + height, OUTER_EDGE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_EDGE);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, PANEL_BG);
        graphics.fill(x + 8, y + 8, x + width - 8, y + 27, PANEL_INNER);

        graphics.fill(x + 8, y + 27, x + width - 8, y + 29, RITUAL_GOLD);
        graphics.fill(x + 12, y + height - 10, x + width - 12, y + height - 8, withAlpha(VOID_PURPLE, 130 + (int) (pulse * 70)));

        renderRunes(graphics, x, y, width, height, pulse);
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

    private void renderSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_SHADOW);
        graphics.fill(x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, SLOT_INNER);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 2, y + 2, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + SLOT_SIZE - 2, SLOT_HIGHLIGHT);
        graphics.fill(x + 2, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, OUTER_EDGE);
        graphics.fill(x + SLOT_SIZE - 2, y + 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, OUTER_EDGE);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, float pulse) {
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + SCROLLBAR_HEIGHT, SCROLLBAR_BG);
        graphics.fill(x + 2, y + 2, x + SCROLLBAR_WIDTH - 2, y + SCROLLBAR_HEIGHT - 2, OUTER_EDGE);
        graphics.fill(x + 4, y + 4, x + SCROLLBAR_WIDTH - 4, y + SCROLLBAR_HEIGHT - 4, PANEL_INNER);

        int thumbY = y + 1 + (int) ((SCROLLBAR_HEIGHT - SCROLLER_HEIGHT - 2) * scrollOffset);
        int thumbColor = canScroll() ? withAlpha(VOID_PURPLE, 185 + (int) (pulse * 55)) : 0xFF3A3443;
        if (canScroll()) {
            graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + SCROLLER_HEIGHT, OUTER_EDGE);
            graphics.fill(x + 2, thumbY + 1, x + SCROLLBAR_WIDTH - 2, thumbY + SCROLLER_HEIGHT - 1, thumbColor);
            graphics.fill(x + 3, thumbY + 2, x + SCROLLBAR_WIDTH - 3, thumbY + 3, ELDRITCH_TEAL);
            graphics.fill(x + 3, thumbY + SCROLLER_HEIGHT - 3, x + SCROLLBAR_WIDTH - 3, thumbY + SCROLLER_HEIGHT - 2, RITUAL_GOLD);
        } else {
            graphics.fill(x + 2, thumbY + 1, x + SCROLLBAR_WIDTH - 2, thumbY + SCROLLER_HEIGHT - 1, thumbColor);
        }
    }

    private void renderSectionFrame(GuiGraphics graphics, int x, int y, int width, int height, float pulse) {
        graphics.fill(x, y, x + width, y + height, OUTER_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, SECTION_EDGE);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, SECTION_BG);

        int accent = withAlpha(VOID_PURPLE, 90 + (int) (pulse * 65));
        graphics.fill(x + 5, y + 4, x + width - 5, y + 5, accent);
        graphics.fill(x + 5, y + height - 5, x + width - 5, y + height - 4, withAlpha(ELDRITCH_TEAL, 70));
    }

    private void renderSearchFrame(GuiGraphics graphics, int x, int y, int width, int height, float pulse) {
        graphics.fill(x, y, x + width, y + height, OUTER_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, SECTION_EDGE);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, SEARCH_BG);
        graphics.fill(x + 4, y + height - 3, x + width - 4, y + height - 2, withAlpha(ELDRITCH_TEAL, 120 + (int) (pulse * 70)));
    }

    private void renderRitualDivider(GuiGraphics graphics, int x, int y, int width, float pulse) {
        graphics.fill(x, y, x + width, y + 1, OUTER_EDGE);
        graphics.fill(x, y + 1, x + width, y + 2, withAlpha(RITUAL_GOLD, 135 + (int) (pulse * 70)));
        for (int i = 0; i < width; i += 18) {
            graphics.fill(x + i + 7, y - 1, x + i + 11, y + 3, OUTER_EDGE);
            graphics.fill(x + i + 8, y, x + i + 10, y + 2, VOID_PURPLE);
        }
    }

    private void renderRunes(GuiGraphics graphics, int x, int y, int width, int height, float pulse) {
        int color = withAlpha(TEXT_MUTED, 70 + (int) (pulse * 45));
        graphics.drawString(this.font, "I", x + 16, y + height - 21, color, false);
        graphics.drawString(this.font, "V", x + 29, y + height - 21, color, false);
        graphics.drawString(this.font, "X", x + width - 32, y + height - 21, color, false);
        graphics.drawString(this.font, ".", x + width - 18, y + height - 21, color, false);
    }

    private float getPulse(float partialTick) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return 0.5F;
        }
        return 0.5F + 0.5F * Mth.sin((this.minecraft.level.getGameTime() + partialTick) * 0.09F);
    }

    private int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
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
