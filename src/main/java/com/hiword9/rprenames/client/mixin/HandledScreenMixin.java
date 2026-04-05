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
    @Unique private static final int RPR_VISIBLE_ROWS = 6;
    @Unique private static final int RPR_HEADER_HEIGHT = 12;
    @Unique private static final int RPR_PREVIEW_HEIGHT = 24;
    @Unique private static final int RPR_ROW_HEIGHT = 12;
    @Unique private static final int RPR_PADDING = 4;
    @Unique private static final int RPR_PAGE_BUTTON_WIDTH = 12;
    @Unique private static final int RPR_PAGE_BUTTON_HEIGHT = 12;

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
        context.drawText(client.textRenderer, Text.translatable("rprenames.preview"), previewBoxX + 3, previewBoxY + 3, 0xFFAAAAAA, false);

        String previewText = rprenames$getPreviewText(mouseX, mouseY);
        int previewTextWidth = previewBoxWidth - 6;
        String previewLine = client.textRenderer.trimToWidth(previewText, previewTextWidth);
        if (client.textRenderer.getWidth(previewLine) < client.textRenderer.getWidth(previewText)) {
            previewLine = client.textRenderer.trimToWidth(previewText, Math.max(0, previewTextWidth - client.textRenderer.getWidth("..."))) + "...";
        }
        context.drawText(client.textRenderer, previewLine, previewBoxX + 3, previewBoxY + 13, 0xFFFFFFFF, false);

        int rowY = previewBoxY + RPR_PREVIEW_HEIGHT + RPR_PADDING;

        if (rprenames$visibleEntries.isEmpty()) {
            String messageKey = rprenames$hasInputItem ? "rprenames.no_matches" : "rprenames.empty_slot";
            context.drawText(client.textRenderer, Text.translatable(messageKey), rprenames$panelLocalX + RPR_PADDING, rowY, 0xFFAAAAAA, false);
        } else {
            String activeName = rprenames$getCurrentNameFieldText();

            for (int i = 0; i < rprenames$visibleEntries.size(); i++) {
                String entry = rprenames$visibleEntries.get(i);
                int absoluteRowY = panelAbsY + rowY;
                boolean hovered = mouseX >= panelAbsX + 2
                        && mouseX <= panelAbsX + RPR_PANEL_WIDTH - 3
                        && mouseY >= absoluteRowY - 1
                        && mouseY <= absoluteRowY + 9;
                boolean selected = entry.equals(activeName) || entry.equals(rprenames$previewEntry);

                if (hovered) {
                    context.fill(rprenames$panelLocalX + 2, rowY - 1, rprenames$panelLocalX + RPR_PANEL_WIDTH - 3, rowY + 10, 0x443A78D8);
                } else if (selected) {
                    context.fill(rprenames$panelLocalX + 2, rowY - 1, rprenames$panelLocalX + RPR_PANEL_WIDTH - 3, rowY + 10, 0x333A78D8);
                }

                String line = client.textRenderer.trimToWidth(entry, RPR_PANEL_WIDTH - (RPR_PADDING * 2) - 4);
                if (client.textRenderer.getWidth(line) < client.textRenderer.getWidth(entry)) {
                    line = client.textRenderer.trimToWidth(entry, Math.max(0, RPR_PANEL_WIDTH - (RPR_PADDING * 2) - 4 - client.textRenderer.getWidth("..."))) + "...";
                }
                context.drawText(client.textRenderer, line, rprenames$panelLocalX + RPR_PADDING, rowY, 0xFFFFFFFF, false);
                rowY += RPR_ROW_HEIGHT;
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

        int rowIndex = rprenames$getHoveredRow(mouseX, mouseY);
        if (rowIndex >= 0 && rowIndex < rprenames$visibleEntries.size() && (Object) this instanceof AnvilScreen) {
            String entry = rprenames$visibleEntries.get(rowIndex);
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

        rprenames$panelHeight = 8 + RPR_HEADER_HEIGHT + RPR_PREVIEW_HEIGHT + (RPR_VISIBLE_ROWS * RPR_ROW_HEIGHT) + RPR_PAGE_BUTTON_HEIGHT + (RPR_PADDING * 2);
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
        String query = rprenames$getCurrentNameFieldText();
        List<String> filtered = RenameCatalog.filter(item, query, Integer.MAX_VALUE);

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
        rprenames$syncPreviewEntry(query);
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
        int hoveredRow = rprenames$getHoveredRow(mouseX, mouseY);
        if (hoveredRow >= 0 && hoveredRow < rprenames$visibleEntries.size()) {
            String hoveredEntry = rprenames$visibleEntries.get(hoveredRow);
            rprenames$previewEntry = hoveredEntry;
            return hoveredEntry;
        }

        if (!rprenames$previewEntry.isBlank()) {
            return rprenames$previewEntry;
        }

        if (!rprenames$visibleEntries.isEmpty()) {
            return rprenames$visibleEntries.get(0);
        }

        return Text.translatable("rprenames.preview_hint").getString();
    }

    @Unique
    private int rprenames$getPageCount() {
        return Math.max(1, (int) Math.ceil((double) rprenames$allEntries.size() / RPR_VISIBLE_ROWS));
    }

    @Unique
    private boolean rprenames$hasMultiplePages() {
        return rprenames$allEntries.size() > RPR_VISIBLE_ROWS;
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
        int start = rprenames$page * RPR_VISIBLE_ROWS;
        int end = Math.min(start + RPR_VISIBLE_ROWS, rprenames$allEntries.size());
        if (start < end) {
            rprenames$visibleEntries.addAll(rprenames$allEntries.subList(start, end));
        }
    }

    @Unique
    private void rprenames$syncPreviewEntry(String query) {
        if (rprenames$allEntries.isEmpty()) {
            rprenames$previewEntry = "";
            return;
        }

        if (!query.isBlank() && rprenames$allEntries.contains(query)) {
            rprenames$previewEntry = query;
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
        if (mouseX < panelAbsX + 2 || mouseX > panelAbsX + RPR_PANEL_WIDTH - 3) {
            return -1;
        }

        int rowsTop = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT + RPR_PREVIEW_HEIGHT + RPR_PADDING;
        int relativeY = (int) mouseY - rowsTop;
        if (relativeY < 0) {
            return -1;
        }

        int rowIndex = relativeY / RPR_ROW_HEIGHT;
        return rowIndex >= 0 && rowIndex < rprenames$visibleEntries.size() ? rowIndex : -1;
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
        rprenames$syncPreviewEntry(rprenames$getCurrentNameFieldText());
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
