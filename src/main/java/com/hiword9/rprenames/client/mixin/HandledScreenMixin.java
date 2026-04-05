package com.hiword9.rprenames.client.mixin;

import java.util.ArrayList;
import java.util.List;

import com.hiword9.rprenames.client.ext.RenamePanelScreenExt;
import com.hiword9.rprenames.client.model.RenameCatalog;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen implements RenamePanelScreenExt {
    @Unique private static final int RPR_PANEL_WIDTH = 152;
    @Unique private static final int RPR_HEADER_HEIGHT = 12;
    @Unique private static final int RPR_PREVIEW_HEIGHT = 48;
    @Unique private static final int RPR_PADDING = 4;
    @Unique private static final int RPR_PAGE_BUTTON_WIDTH = 12;
    @Unique private static final int RPR_PAGE_BUTTON_HEIGHT = 12;
    @Unique private static final int RPR_GRID_COLUMNS = 3;
    @Unique private static final int RPR_GRID_ROWS = 2;
    @Unique private static final int RPR_VISIBLE_ENTRIES = RPR_GRID_COLUMNS * RPR_GRID_ROWS;
    @Unique private static final int RPR_CELL_WIDTH = 44;
    @Unique private static final int RPR_CELL_HEIGHT = 20;
    @Unique private static final int RPR_CELL_GAP = 4;
    @Unique private static final int RPR_GRID_HEIGHT = (RPR_GRID_ROWS * RPR_CELL_HEIGHT) + ((RPR_GRID_ROWS - 1) * RPR_CELL_GAP);

    @Unique private final List<String> rprenames$allEntries = new ArrayList<>();
    @Unique private final List<String> rprenames$visibleEntries = new ArrayList<>();
    @Unique private int rprenames$page = 0;
    @Unique private int rprenames$panelLocalX = -RPR_PANEL_WIDTH - 8;
    @Unique private int rprenames$panelLocalY = 4;
    @Unique private int rprenames$panelHeight = 0;
    @Unique private boolean rprenames$hasInputItem = false;
    @Unique private String rprenames$previewEntry = "";

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void rprenames$drawRenamePanel(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!rprenames$refreshPanelState()) {
            return;
        }

        int panelAbsX = rprenames$getPanelAbsX();
        int panelAbsY = rprenames$getPanelAbsY();

        context.fill(
                rprenames$panelLocalX,
                rprenames$panelLocalY,
                rprenames$panelLocalX + RPR_PANEL_WIDTH,
                rprenames$panelLocalY + rprenames$panelHeight,
                0xCC101010
        );
        rprenames$drawBorder(context, rprenames$panelLocalX, rprenames$panelLocalY, RPR_PANEL_WIDTH, rprenames$panelHeight, 0xFF6AA6FF);
        context.drawText(client.textRenderer, Text.translatable("rprenames.title"), rprenames$panelLocalX + RPR_PADDING, rprenames$panelLocalY + RPR_PADDING, 0xFF6AA6FF, false);

        String countText = String.valueOf(rprenames$allEntries.size());
        int infoX = rprenames$panelLocalX + RPR_PANEL_WIDTH - RPR_PADDING - client.textRenderer.getWidth(countText);
        context.drawText(client.textRenderer, countText, infoX, rprenames$panelLocalY + RPR_PADDING, 0xFFAAAAAA, false);

        int previewBoxX = rprenames$panelLocalX + RPR_PADDING;
        int previewBoxY = rprenames$panelLocalY + RPR_PADDING + RPR_HEADER_HEIGHT;
        int previewBoxWidth = RPR_PANEL_WIDTH - (RPR_PADDING * 2);
        context.fill(previewBoxX, previewBoxY, previewBoxX + previewBoxWidth, previewBoxY + RPR_PREVIEW_HEIGHT, 0x55222222);
        rprenames$drawBorder(context, previewBoxX, previewBoxY, previewBoxWidth, RPR_PREVIEW_HEIGHT, 0x884A4A4A);

        String previewText = rprenames$getPreviewText(mouseX, mouseY);
        if (!previewText.isBlank()) {
            ItemStack previewStack = rprenames$createPreviewStack(previewText);
            int previewItemX = previewBoxX + 10;
            int previewItemY = previewBoxY + 8;
            context.getMatrices().push();
            context.getMatrices().translate(0.0D, 0.0D, 200.0D);
            context.getMatrices().scale(2.0F, 2.0F, 1.0F);
            context.drawItem(previewStack, previewItemX / 2, previewItemY / 2);
            context.getMatrices().pop();

            String caption = client.textRenderer.trimToWidth(previewText, previewBoxWidth - 40);
            if (client.textRenderer.getWidth(caption) < client.textRenderer.getWidth(previewText)) {
                caption = client.textRenderer.trimToWidth(previewText, Math.max(0, previewBoxWidth - 40 - client.textRenderer.getWidth("..."))) + "...";
            }
            context.drawText(client.textRenderer, caption, previewBoxX + 38, previewBoxY + 18, 0xFFFFFFFF, false);
        } else {
            context.drawText(client.textRenderer, Text.translatable("rprenames.preview_hint"), previewBoxX + 6, previewBoxY + 18, 0xFFAAAAAA, false);
        }

        int gridStartY = previewBoxY + RPR_PREVIEW_HEIGHT + RPR_PADDING;
        if (rprenames$visibleEntries.isEmpty()) {
            String messageKey = rprenames$hasInputItem ? "rprenames.no_matches" : "rprenames.empty_slot";
            context.drawText(client.textRenderer, Text.translatable(messageKey), rprenames$panelLocalX + RPR_PADDING, gridStartY + 14, 0xFFAAAAAA, false);
        } else {
            String activeName = rprenames$getCurrentNameFieldText();
            for (int i = 0; i < rprenames$visibleEntries.size(); i++) {
                int col = i % RPR_GRID_COLUMNS;
                int row = i / RPR_GRID_COLUMNS;
                int cellX = rprenames$panelLocalX + RPR_PADDING + col * (RPR_CELL_WIDTH + RPR_CELL_GAP);
                int cellY = gridStartY + row * (RPR_CELL_HEIGHT + RPR_CELL_GAP);
                int cellAbsX = panelAbsX + RPR_PADDING + col * (RPR_CELL_WIDTH + RPR_CELL_GAP);
                int cellAbsY = panelAbsY + gridStartY + row * (RPR_CELL_HEIGHT + RPR_CELL_GAP);
                String entry = rprenames$visibleEntries.get(i);
                boolean hovered = mouseX >= cellAbsX
                        && mouseX <= cellAbsX + RPR_CELL_WIDTH
                        && mouseY >= cellAbsY
                        && mouseY <= cellAbsY + RPR_CELL_HEIGHT;
                boolean selected = entry.equals(activeName) || entry.equals(rprenames$previewEntry);

                int background = hovered ? 0x443A78D8 : selected ? 0x333A78D8 : 0x55222222;
                int border = hovered || selected ? 0xFF6AA6FF : 0x884A4A4A;
                context.fill(cellX, cellY, cellX + RPR_CELL_WIDTH, cellY + RPR_CELL_HEIGHT, background);
                rprenames$drawBorder(context, cellX, cellY, RPR_CELL_WIDTH, RPR_CELL_HEIGHT, border);

                ItemStack entryStack = rprenames$createPreviewStack(entry);
                context.drawItem(entryStack, cellX + 14, cellY + 2);
            }
        }

        if (rprenames$hasMultiplePages()) {
            int footerY = rprenames$panelLocalY + rprenames$panelHeight - RPR_PADDING - RPR_PAGE_BUTTON_HEIGHT;
            int leftButtonX = rprenames$panelLocalX + RPR_PADDING;
            int rightButtonX = rprenames$panelLocalX + RPR_PANEL_WIDTH - RPR_PADDING - RPR_PAGE_BUTTON_WIDTH;
            int indicatorY = footerY + 2;

            context.fill(leftButtonX, footerY, leftButtonX + RPR_PAGE_BUTTON_WIDTH, footerY + RPR_PAGE_BUTTON_HEIGHT, 0x66333333);
            context.fill(rightButtonX, footerY, rightButtonX + RPR_PAGE_BUTTON_WIDTH, footerY + RPR_PAGE_BUTTON_HEIGHT, 0x66333333);
            context.drawText(client.textRenderer, "<", leftButtonX + 3, footerY + 2, 0xFFFFFFFF, false);
            context.drawText(client.textRenderer, ">", rightButtonX + 3, footerY + 2, 0xFFFFFFFF, false);

            String pageText = (rprenames$page + 1) + "/" + rprenames$getPageCount();
            int pageTextX = rprenames$panelLocalX + (RPR_PANEL_WIDTH / 2) - (client.textRenderer.getWidth(pageText) / 2);
            context.drawText(client.textRenderer, pageText, pageTextX, indicatorY, 0xFFAAAAAA, false);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button != 0 || !rprenames$refreshPanelState() || !rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        int pageAction = rprenames$getPageClickAction(mouseX, mouseY);
        if (pageAction != 0) {
            rprenames$changePage(pageAction);
            cir.setReturnValue(true);
            return;
        }

        int hoveredIndex = rprenames$getHoveredRow(mouseX, mouseY);
        if (hoveredIndex >= 0 && hoveredIndex < rprenames$visibleEntries.size() && (Object) this instanceof AnvilScreen) {
            String entry = rprenames$visibleEntries.get(hoveredIndex);
            TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
            nameField.setText(entry);
            rprenames$previewEntry = entry;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!rprenames$refreshPanelState() || !rprenames$hasMultiplePages() || !rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        if (verticalAmount < 0) {
            rprenames$changePage(1);
        } else if (verticalAmount > 0) {
            rprenames$changePage(-1);
        } else {
            return;
        }

        cir.setReturnValue(true);
    }

    @Override
    @Unique
    public boolean rprenames$refreshPanelState() {
        if (client == null || client.player == null || !((Object) this instanceof AnvilScreen anvilScreen)) {
            rprenames$clearState();
            return false;
        }

        rprenames$panelHeight = 8 + RPR_HEADER_HEIGHT + RPR_PREVIEW_HEIGHT + RPR_GRID_HEIGHT + RPR_PAGE_BUTTON_HEIGHT + (RPR_PADDING * 4);
        ItemStack input = anvilScreen.getScreenHandler().getSlot(0).getStack();
        rprenames$hasInputItem = !input.isEmpty();

        if (input.isEmpty()) {
            rprenames$allEntries.clear();
            rprenames$visibleEntries.clear();
            rprenames$page = 0;
            rprenames$previewEntry = "";
            return true;
        }

        Item item = input.getItem();
        List<String> filtered = RenameCatalog.filter(item, "", Integer.MAX_VALUE);

        rprenames$allEntries.clear();
        rprenames$allEntries.addAll(filtered);

        int maxPage = Math.max(0, rprenames$getPageCount() - 1);
        if (rprenames$page > maxPage) {
            rprenames$page = maxPage;
        }
        if (rprenames$page < 0) {
            rprenames$page = 0;
        }

        rprenames$refreshVisibleEntries();
        rprenames$syncPreviewEntry();
        return true;
    }

    @Unique
    private int rprenames$getPanelAbsX() {
        return ((HandledScreenAccessor) (Object) this).rprenames$getX() + rprenames$panelLocalX;
    }

    @Unique
    private int rprenames$getPanelAbsY() {
        return ((HandledScreenAccessor) (Object) this).rprenames$getY() + rprenames$panelLocalY;
    }

    @Unique
    private String rprenames$getPreviewText(int mouseX, int mouseY) {
        int hoveredIndex = rprenames$getHoveredRow(mouseX, mouseY);
        if (hoveredIndex >= 0 && hoveredIndex < rprenames$visibleEntries.size()) {
            String hoveredEntry = rprenames$visibleEntries.get(hoveredIndex);
            rprenames$previewEntry = hoveredEntry;
            return hoveredEntry;
        }

        if (!rprenames$previewEntry.isBlank()) {
            return rprenames$previewEntry;
        }

        if (!rprenames$visibleEntries.isEmpty()) {
            return rprenames$visibleEntries.get(0);
        }

        return "";
    }

    @Unique
    private ItemStack rprenames$createPreviewStack(String entry) {
        if (!((Object) this instanceof AnvilScreen anvilScreen)) {
            return ItemStack.EMPTY;
        }

        ItemStack source = anvilScreen.getScreenHandler().getSlot(0).getStack();
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack preview = source.copy();
        preview.set(DataComponentTypes.CUSTOM_NAME, Text.literal(entry));
        return preview;
    }

    @Unique
    private int rprenames$getPageCount() {
        return Math.max(1, (int) Math.ceil((double) rprenames$allEntries.size() / RPR_VISIBLE_ENTRIES));
    }

    @Unique
    private boolean rprenames$hasMultiplePages() {
        return rprenames$allEntries.size() > RPR_VISIBLE_ENTRIES;
    }

    @Unique
    private int rprenames$getPageClickAction(double mouseX, double mouseY) {
        if (!rprenames$hasMultiplePages()) {
            return 0;
        }

        int panelAbsX = rprenames$getPanelAbsX();
        int panelAbsY = rprenames$getPanelAbsY();
        int footerY = panelAbsY + rprenames$panelHeight - RPR_PADDING - RPR_PAGE_BUTTON_HEIGHT;
        int leftButtonX = panelAbsX + RPR_PADDING;
        int rightButtonX = panelAbsX + RPR_PANEL_WIDTH - RPR_PADDING - RPR_PAGE_BUTTON_WIDTH;

        if (mouseY >= footerY && mouseY <= footerY + RPR_PAGE_BUTTON_HEIGHT) {
            if (mouseX >= leftButtonX && mouseX <= leftButtonX + RPR_PAGE_BUTTON_WIDTH) {
                return -1;
            }
            if (mouseX >= rightButtonX && mouseX <= rightButtonX + RPR_PAGE_BUTTON_WIDTH) {
                return 1;
            }
        }

        return 0;
    }

    @Unique
    private void rprenames$refreshVisibleEntries() {
        rprenames$visibleEntries.clear();
        int start = rprenames$page * RPR_VISIBLE_ENTRIES;
        int end = Math.min(start + RPR_VISIBLE_ENTRIES, rprenames$allEntries.size());
        if (start < end) {
            rprenames$visibleEntries.addAll(rprenames$allEntries.subList(start, end));
        }
    }

    @Unique
    private void rprenames$syncPreviewEntry() {
        if (rprenames$allEntries.isEmpty()) {
            rprenames$previewEntry = "";
            return;
        }

        String currentName = rprenames$getCurrentNameFieldText();
        if (!currentName.isBlank() && rprenames$allEntries.contains(currentName)) {
            rprenames$previewEntry = currentName;
            return;
        }

        if (rprenames$previewEntry.isBlank() || !rprenames$allEntries.contains(rprenames$previewEntry)) {
            rprenames$previewEntry = rprenames$visibleEntries.isEmpty() ? rprenames$allEntries.get(0) : rprenames$visibleEntries.get(0);
        }
    }

    @Unique
    private String rprenames$getCurrentNameFieldText() {
        if (!((Object) this instanceof AnvilScreen)) {
            return "";
        }

        TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
        return nameField == null ? "" : nameField.getText();
    }

    @Override
    @Unique
    public boolean rprenames$canScroll() {
        return rprenames$hasMultiplePages();
    }

    @Override
    @Unique
    public boolean rprenames$isInsidePanel(double mouseX, double mouseY) {
        int panelAbsX = rprenames$getPanelAbsX();
        int panelAbsY = rprenames$getPanelAbsY();
        return mouseX >= panelAbsX
                && mouseX <= panelAbsX + RPR_PANEL_WIDTH
                && mouseY >= panelAbsY
                && mouseY <= panelAbsY + rprenames$panelHeight;
    }

    @Override
    @Unique
    public int rprenames$getHoveredRow(double mouseX, double mouseY) {
        int panelAbsX = rprenames$getPanelAbsX();
        int panelAbsY = rprenames$getPanelAbsY();
        int gridAbsX = panelAbsX + RPR_PADDING;
        int gridAbsY = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT + RPR_PREVIEW_HEIGHT + RPR_PADDING;

        for (int i = 0; i < rprenames$visibleEntries.size(); i++) {
            int col = i % RPR_GRID_COLUMNS;
            int row = i / RPR_GRID_COLUMNS;
            int cellAbsX = gridAbsX + col * (RPR_CELL_WIDTH + RPR_CELL_GAP);
            int cellAbsY = gridAbsY + row * (RPR_CELL_HEIGHT + RPR_CELL_GAP);
            if (mouseX >= cellAbsX && mouseX <= cellAbsX + RPR_CELL_WIDTH && mouseY >= cellAbsY && mouseY <= cellAbsY + RPR_CELL_HEIGHT) {
                return i;
            }
        }

        return -1;
    }

    @Override
    @Unique
    public List<String> rprenames$getVisibleEntries() {
        return List.copyOf(rprenames$visibleEntries);
    }

    @Override
    @Unique
    public void rprenames$scroll(int amount) {
        rprenames$changePage(amount);
    }

    @Unique
    private void rprenames$changePage(int amount) {
        int maxPage = Math.max(0, rprenames$getPageCount() - 1);
        rprenames$page = Math.max(0, Math.min(maxPage, rprenames$page + amount));
        rprenames$refreshVisibleEntries();
        rprenames$syncPreviewEntry();
    }

    @Unique
    private void rprenames$clearState() {
        rprenames$allEntries.clear();
        rprenames$visibleEntries.clear();
        rprenames$page = 0;
        rprenames$panelHeight = 0;
        rprenames$hasInputItem = false;
        rprenames$previewEntry = "";
    }

    @Unique
    private static void rprenames$drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
