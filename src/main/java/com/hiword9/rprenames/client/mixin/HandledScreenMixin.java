package com.hiword9.rprenames.client.mixin;

import java.util.ArrayList;
import java.util.List;

import com.hiword9.rprenames.client.model.RenameCatalog;

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

    @Unique private final List<String> rprenames$allEntries = new ArrayList<>();
    @Unique private final List<String> rprenames$visibleEntries = new ArrayList<>();
    @Unique private int rprenames$scrollOffset = 0;
    @Unique private int rprenames$panelLocalX = -RPR_PANEL_WIDTH - 8;
    @Unique private int rprenames$panelLocalY = 4;
    @Unique private int rprenames$panelHeight = 0;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void rprenames$drawRenamePanel(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!rprenames$refreshPanelState()) {
            return;
        }

        int screenX = ((HandledScreenAccessor) (Object) this).rprenames$getX();
        int screenY = ((HandledScreenAccessor) (Object) this).rprenames$getY();
        int panelAbsX = screenX + rprenames$panelLocalX;
        int panelAbsY = screenY + rprenames$panelLocalY;

        context.fill(
                rprenames$panelLocalX,
                rprenames$panelLocalY,
                rprenames$panelLocalX + RPR_PANEL_WIDTH,
                rprenames$panelLocalY + rprenames$panelHeight,
                0xCC101010
        );
        rprenames$drawBorder(context, rprenames$panelLocalX, rprenames$panelLocalY, RPR_PANEL_WIDTH, rprenames$panelHeight, 0xFF6AA6FF);
        context.drawText(client.textRenderer, Text.translatable("rprenames.title"), rprenames$panelLocalX + RPR_PADDING, rprenames$panelLocalY + RPR_PADDING, 0xFF6AA6FF, false);

        int infoX = rprenames$panelLocalX + RPR_PANEL_WIDTH - RPR_PADDING - client.textRenderer.getWidth(rprenames$allEntries.size() + "");
        context.drawText(client.textRenderer, String.valueOf(rprenames$allEntries.size()), infoX, rprenames$panelLocalY + RPR_PADDING, 0xFFAAAAAA, false);

        int rowY = rprenames$panelLocalY + RPR_PADDING + RPR_HEADER_HEIGHT;
        for (int i = 0; i < rprenames$visibleEntries.size(); i++) {
            String entry = rprenames$visibleEntries.get(i);
            int absoluteRowY = panelAbsY + RPR_PADDING + RPR_HEADER_HEIGHT + i * RPR_ROW_HEIGHT;
            boolean hovered = mouseX >= panelAbsX
                    && mouseX <= panelAbsX + RPR_PANEL_WIDTH
                    && mouseY >= absoluteRowY - 1
                    && mouseY <= absoluteRowY + 9;
            if (hovered) {
                context.fill(rprenames$panelLocalX + 2, rowY - 1, rprenames$panelLocalX + RPR_PANEL_WIDTH - 2, rowY + 10, 0x443A78D8);
            }
            context.drawText(client.textRenderer, entry, rprenames$panelLocalX + RPR_PADDING, rowY, 0xFFFFFFFF, false);
            rowY += RPR_ROW_HEIGHT;
        }

        if (rprenames$canScroll()) {
            int barX = rprenames$panelLocalX + RPR_PANEL_WIDTH - 5;
            int trackTop = rprenames$panelLocalY + RPR_PADDING + RPR_HEADER_HEIGHT;
            int trackHeight = RPR_VISIBLE_ROWS * RPR_ROW_HEIGHT;
            context.fill(barX, trackTop, barX + 2, trackTop + trackHeight, 0x66333333);

            int maxOffset = Math.max(1, rprenames$allEntries.size() - RPR_VISIBLE_ROWS);
            int thumbHeight = Math.max(14, (int) Math.floor((double) trackHeight * RPR_VISIBLE_ROWS / rprenames$allEntries.size()));
            int travel = Math.max(1, trackHeight - thumbHeight);
            int thumbOffset = (int) Math.round((double) travel * rprenames$scrollOffset / maxOffset);
            context.fill(barX, trackTop + thumbOffset, barX + 2, trackTop + thumbOffset + thumbHeight, 0xFF6AA6FF);
        }
    }


    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !rprenames$refreshPanelState() || !rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        int rowIndex = rprenames$getHoveredRow(mouseX, mouseY);
        if (rowIndex >= 0 && rowIndex < rprenames$visibleEntries.size()) {
            TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
            nameField.setText(rprenames$visibleEntries.get(rowIndex));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!rprenames$refreshPanelState() || !rprenames$canScroll() || !rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        if (verticalAmount < 0) {
            rprenames$scroll(1);
            cir.setReturnValue(true);
        } else if (verticalAmount > 0) {
            rprenames$scroll(-1);
            cir.setReturnValue(true);
        }
    }


    @Override
    @Unique
    public boolean rprenames$refreshPanelState() {
        if (client == null || client.player == null || !((Object) this instanceof AnvilScreen anvilScreen)) {
            rprenames$clearState();
            return false;
        }

        ItemStack input = anvilScreen.getScreenHandler().getSlot(0).getStack();
        if (input.isEmpty()) {
            rprenames$clearState();
            return false;
        }

        TextFieldWidget nameField = ((AnvilScreenAccessor) (Object) this).rprenames$getNameField();
        String query = nameField.getText();
        Item item = input.getItem();
        List<String> filtered = RenameCatalog.filter(item, query, Integer.MAX_VALUE);
        if (filtered.isEmpty()) {
            rprenames$clearState();
            return false;
        }

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
        rprenames$panelHeight = 8 + RPR_HEADER_HEIGHT + (rprenames$visibleEntries.size() * RPR_ROW_HEIGHT);
        return !rprenames$visibleEntries.isEmpty();
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
        int screenX = ((HandledScreenAccessor) (Object) this).rprenames$getX();
        int screenY = ((HandledScreenAccessor) (Object) this).rprenames$getY();
        int panelAbsX = screenX + rprenames$panelLocalX;
        int panelAbsY = screenY + rprenames$panelLocalY;
        return mouseX >= panelAbsX
                && mouseX <= panelAbsX + RPR_PANEL_WIDTH
                && mouseY >= panelAbsY
                && mouseY <= panelAbsY + rprenames$panelHeight;
    }

    @Override
    @Unique
    public int rprenames$getHoveredRow(double mouseX, double mouseY) {
        int screenX = ((HandledScreenAccessor) (Object) this).rprenames$getX();
        int screenY = ((HandledScreenAccessor) (Object) this).rprenames$getY();
        int panelAbsX = screenX + rprenames$panelLocalX;
        int panelAbsY = screenY + rprenames$panelLocalY;
        if (mouseX < panelAbsX || mouseX > panelAbsX + RPR_PANEL_WIDTH) {
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
    public java.util.List<String> rprenames$getVisibleEntries() {
        return java.util.List.copyOf(rprenames$visibleEntries);
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
    }

    @Unique
    private static void rprenames$drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
