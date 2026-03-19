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
    @Unique private static final int RPR_PANEL_WIDTH = 132;
    @Unique private static final int RPR_VISIBLE_ROWS = 8;
    @Unique private static final int RPR_HEADER_HEIGHT = 12;
    @Unique private static final int RPR_ROW_HEIGHT = 12;
    @Unique private static final int RPR_PADDING = 4;
    @Unique private static final int RPR_SCROLLBAR_WIDTH = 8;
    @Unique private static final int RPR_SCROLL_BUTTON_HEIGHT = 10;

    @Unique private final List<String> rprenames$allEntries = new ArrayList<>();
    @Unique private final List<String> rprenames$visibleEntries = new ArrayList<>();
    @Unique private int rprenames$scrollOffset = 0;
    @Unique private int rprenames$panelLocalX = -RPR_PANEL_WIDTH - 8;
    @Unique private int rprenames$panelLocalY = 4;
    @Unique private int rprenames$panelHeight = 0;
    @Unique private boolean rprenames$hasInputItem = false;

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

        int rowY = rprenames$panelLocalY + RPR_PADDING + RPR_HEADER_HEIGHT;
        int textColor = 0xFFFFFFFF;

        if (rprenames$visibleEntries.isEmpty()) {
            String message = rprenames$hasInputItem ? "No renames found" : "Put item into left slot";
            context.drawText(client.textRenderer, message, rprenames$panelLocalX + RPR_PADDING, rowY, 0xFFAAAAAA, false);
        } else {
            for (int i = 0; i < rprenames$visibleEntries.size(); i++) {
                String entry = rprenames$visibleEntries.get(i);
                int absoluteRowY = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT + i * RPR_ROW_HEIGHT;
                boolean hovered = mouseX >= panelAbsX
                        && mouseX <= panelAbsX + RPR_PANEL_WIDTH - RPR_SCROLLBAR_WIDTH - 2
                        && mouseY >= absoluteRowY - 1
                        && mouseY <= absoluteRowY + 9;
                if (hovered) {
                    context.fill(rprenames$panelLocalX + 2, rowY - 1, rprenames$panelLocalX + RPR_PANEL_WIDTH - RPR_SCROLLBAR_WIDTH - 3, rowY + 10, 0x443A78D8);
                }
                context.drawText(client.textRenderer, entry, rprenames$panelLocalX + RPR_PADDING, rowY, textColor, false);
                rowY += RPR_ROW_HEIGHT;
            }
        }

        if (rprenames$canScroll()) {
            int barX = rprenames$panelLocalX + RPR_PANEL_WIDTH - RPR_SCROLLBAR_WIDTH - 2;
            int upY = rprenames$panelLocalY + RPR_PADDING + RPR_HEADER_HEIGHT;
            int downY = rprenames$panelLocalY + rprenames$panelHeight - RPR_PADDING - RPR_SCROLL_BUTTON_HEIGHT;
            int trackTop = upY + RPR_SCROLL_BUTTON_HEIGHT + 2;
            int trackBottom = downY - 2;
            int trackHeight = Math.max(8, trackBottom - trackTop);

            context.fill(barX, upY, barX + RPR_SCROLLBAR_WIDTH, upY + RPR_SCROLL_BUTTON_HEIGHT, 0x66333333);
            context.fill(barX, downY, barX + RPR_SCROLLBAR_WIDTH, downY + RPR_SCROLL_BUTTON_HEIGHT, 0x66333333);
            context.drawText(client.textRenderer, "^", barX + 2, upY + 1, 0xFFFFFFFF, false);
            context.drawText(client.textRenderer, "v", barX + 2, downY + 1, 0xFFFFFFFF, false);

            context.fill(barX + 2, trackTop, barX + RPR_SCROLLBAR_WIDTH - 2, trackTop + trackHeight, 0x66333333);

            int maxOffset = Math.max(1, rprenames$allEntries.size() - RPR_VISIBLE_ROWS);
            int thumbHeight = Math.max(14, (int) Math.floor((double) trackHeight * RPR_VISIBLE_ROWS / rprenames$allEntries.size()));
            int travel = Math.max(1, trackHeight - thumbHeight);
            int thumbOffset = (int) Math.round((double) travel * rprenames$scrollOffset / maxOffset);
            context.fill(barX + 1, trackTop + thumbOffset, barX + RPR_SCROLLBAR_WIDTH - 1, trackTop + thumbOffset + thumbHeight, 0xFF6AA6FF);
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

        if (rprenames$canScroll()) {
            int action = rprenames$getScrollClickAction(mouseX, mouseY);
            if (action != 0) {
                rprenames$scroll(action);
                cir.setReturnValue(true);
                return;
            }
        }

        int rowIndex = rprenames$getHoveredRow(mouseX, mouseY);
        if (rowIndex >= 0 && rowIndex < rprenames$visibleEntries.size() && (Object) this instanceof AnvilScreen) {
            TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
            nameField.setText(rprenames$visibleEntries.get(rowIndex));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!rprenames$refreshPanelState() || !rprenames$canScroll() || !rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        if (verticalAmount < 0) {
            rprenames$scroll(1);
        } else if (verticalAmount > 0) {
            rprenames$scroll(-1);
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

        rprenames$panelHeight = 8 + RPR_HEADER_HEIGHT + (RPR_VISIBLE_ROWS * RPR_ROW_HEIGHT);
        ItemStack input = anvilScreen.getScreenHandler().getSlot(0).getStack();
        rprenames$hasInputItem = !input.isEmpty();

        if (input.isEmpty()) {
            rprenames$allEntries.clear();
            rprenames$visibleEntries.clear();
            rprenames$scrollOffset = 0;
            return true;
        }

        TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
        String query = nameField.getText();
        Item item = input.getItem();
        List<String> filtered = RenameCatalog.filter(item, query, Integer.MAX_VALUE);

        rprenames$allEntries.clear();
        rprenames$allEntries.addAll(filtered);
        int maxOffset = Math.max(0, rprenames$allEntries.size() - RPR_VISIBLE_ROWS);
        if (rprenames$scrollOffset > maxOffset) {
            rprenames$scrollOffset = maxOffset;
        }
        if (rprenames$scrollOffset < 0) {
            rprenames$scrollOffset = 0;
        }

        rprenames$refreshVisibleEntries();
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
    private int rprenames$getScrollClickAction(double mouseX, double mouseY) {
        int panelAbsX = rprenames$getPanelAbsX();
        int panelAbsY = rprenames$getPanelAbsY();
        int barX = panelAbsX + RPR_PANEL_WIDTH - RPR_SCROLLBAR_WIDTH - 2;
        int upY = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT;
        int downY = panelAbsY + rprenames$panelHeight - RPR_PADDING - RPR_SCROLL_BUTTON_HEIGHT;
        int trackTop = upY + RPR_SCROLL_BUTTON_HEIGHT + 2;
        int trackBottom = downY - 2;

        if (mouseX < barX || mouseX > barX + RPR_SCROLLBAR_WIDTH) {
            return 0;
        }
        if (mouseY >= upY && mouseY <= upY + RPR_SCROLL_BUTTON_HEIGHT) {
            return -1;
        }
        if (mouseY >= downY && mouseY <= downY + RPR_SCROLL_BUTTON_HEIGHT) {
            return 1;
        }
        if (mouseY >= trackTop && mouseY <= trackBottom) {
            int thumbCenter = rprenames$getThumbCenter(trackTop, trackBottom);
            return mouseY < thumbCenter ? -RPR_VISIBLE_ROWS : RPR_VISIBLE_ROWS;
        }
        return 0;
    }

    @Unique
    private int rprenames$getThumbCenter(int trackTop, int trackBottom) {
        int trackHeight = Math.max(8, trackBottom - trackTop);
        int maxOffset = Math.max(1, rprenames$allEntries.size() - RPR_VISIBLE_ROWS);
        int thumbHeight = Math.max(14, (int) Math.floor((double) trackHeight * RPR_VISIBLE_ROWS / rprenames$allEntries.size()));
        int travel = Math.max(1, trackHeight - thumbHeight);
        int thumbOffset = (int) Math.round((double) travel * rprenames$scrollOffset / maxOffset);
        return trackTop + thumbOffset + (thumbHeight / 2);
    }

    @Unique
    private void rprenames$refreshVisibleEntries() {
        rprenames$visibleEntries.clear();
        int end = Math.min(rprenames$scrollOffset + RPR_VISIBLE_ROWS, rprenames$allEntries.size());
        if (rprenames$scrollOffset < end) {
            rprenames$visibleEntries.addAll(rprenames$allEntries.subList(rprenames$scrollOffset, end));
        }
    }

    @Override
    @Unique
    public boolean rprenames$canScroll() {
        return rprenames$allEntries.size() > RPR_VISIBLE_ROWS;
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
        int rowRight = panelAbsX + RPR_PANEL_WIDTH - RPR_SCROLLBAR_WIDTH - 3;
        if (mouseX < panelAbsX || mouseX > rowRight) {
            return -1;
        }
        int rowsTop = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT;
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
        int maxOffset = Math.max(0, rprenames$allEntries.size() - RPR_VISIBLE_ROWS);
        rprenames$scrollOffset = Math.max(0, Math.min(maxOffset, rprenames$scrollOffset + amount));
        rprenames$refreshVisibleEntries();
    }

    @Unique
    private void rprenames$clearState() {
        rprenames$allEntries.clear();
        rprenames$visibleEntries.clear();
        rprenames$scrollOffset = 0;
        rprenames$panelHeight = 0;
        rprenames$hasInputItem = false;
    }

    @Unique
    private static void rprenames$drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
