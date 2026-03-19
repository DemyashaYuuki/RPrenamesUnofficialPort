package com.hiword9.rprenames.client.mixin;

import java.util.ArrayList;
import java.util.List;

import com.hiword9.rprenames.client.model.RenameCatalog;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends Screen {
    @Shadow private TextFieldWidget nameField;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

        @Unique private static final int RPR_BOX_WIDTH = 112;
    @Unique private static final int RPR_MAX_ROWS = 8;
    @Unique private List<String> rprenames$visibleEntries = List.of();
    @Unique private int rprenames$boxX;
    @Unique private int rprenames$boxY;
    @Unique private int rprenames$rowHeight = 12;

    protected AnvilScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void rprenames$drawSuggestions(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (client == null || client.player == null) {
            return;
        }

        ItemStack input = ((AnvilScreen) (Object) this).getScreenHandler().getSlot(0).getStack();
        if (input.isEmpty()) {
            rprenames$visibleEntries = List.of();
            return;
        }

        String query = nameField.getText();
        List<String> filtered = RenameCatalog.filter(input.getItem(), query, RPR_MAX_ROWS);
        if (filtered.isEmpty()) {
            rprenames$visibleEntries = List.of();
            return;
        }

        List<String> allMatches = RenameCatalog.filter(input.getItem(), query, Integer.MAX_VALUE);
        int totalMatches = allMatches.size();
        int hidden = Math.max(0, totalMatches - filtered.size());

        rprenames$visibleEntries = new ArrayList<>(filtered);
        rprenames$boxX = this.x - RPR_BOX_WIDTH - 6;
        rprenames$boxY = this.y + 4;

        int rows = filtered.size() + 1 + (hidden > 0 ? 1 : 0);
        int height = 8 + rows * rprenames$rowHeight;

        context.fill(rprenames$boxX, rprenames$boxY, rprenames$boxX + RPR_BOX_WIDTH, rprenames$boxY + height, 0xCC101010);
        rprenames$drawBorder(context, rprenames$boxX, rprenames$boxY, RPR_BOX_WIDTH, height, 0xFF6AA6FF);
        context.drawText(client.textRenderer, Text.translatable("rprenames.title"), rprenames$boxX + 4, rprenames$boxY + 4, 0xFF6AA6FF, false);

        int textY = rprenames$boxY + 4 + rprenames$rowHeight;
        for (String entry : filtered) {
            boolean hovered = mouseX >= rprenames$boxX && mouseX <= rprenames$boxX + RPR_BOX_WIDTH
                    && mouseY >= textY - 1 && mouseY <= textY + 9;
            if (hovered) {
                context.fill(rprenames$boxX + 2, textY - 1, rprenames$boxX + RPR_BOX_WIDTH - 2, textY + 10, 0x443A78D8);
            }
            context.drawText(client.textRenderer, entry, rprenames$boxX + 4, textY, 0xFFFFFFFF, false);
            textY += rprenames$rowHeight;
        }

        if (hidden > 0) {
            context.drawText(
                    client.textRenderer,
                    Text.translatable("rprenames.more", hidden),
                    rprenames$boxX + 4,
                    textY,
                    0xFFAAAAAA,
                    false
            );
        }
    }

    @Unique
    private static void rprenames$drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rprenames$clickSuggestion(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || rprenames$visibleEntries.isEmpty()) {
            return;
        }

        int rowY = rprenames$boxY + 4 + rprenames$rowHeight;
        for (String entry : rprenames$visibleEntries) {
            boolean inside = mouseX >= rprenames$boxX && mouseX <= rprenames$boxX + RPR_BOX_WIDTH
                    && mouseY >= rowY - 1 && mouseY <= rowY + 9;
            if (inside) {
                nameField.setText(entry);
                cir.setReturnValue(true);
                return;
            }
            rowY += rprenames$rowHeight;
        }
    }
}
