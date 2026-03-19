package com.hiword9.rprenames.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof AnvilScreen) || !(self instanceof RenamePanelScreenExt ext) || !(self instanceof AnvilScreenAccessor accessor)) {
            return;
        }
        if (button != 0 || !ext.rprenames$refreshPanelState() || !ext.rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        int rowIndex = ext.rprenames$getHoveredRow(mouseX, mouseY);
        if (rowIndex >= 0 && rowIndex < ext.rprenames$getVisibleEntries().size()) {
            TextFieldWidget nameField = accessor.rprenames$getNameField();
            nameField.setText(ext.rprenames$getVisibleEntries().get(rowIndex));
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void rprenames$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof AnvilScreen) || !(self instanceof RenamePanelScreenExt ext)) {
            return;
        }
        if (!ext.rprenames$refreshPanelState() || !ext.rprenames$canScroll() || !ext.rprenames$isInsidePanel(mouseX, mouseY)) {
            return;
        }

        if (verticalAmount < 0) {
            ext.rprenames$scroll(1);
        } else if (verticalAmount > 0) {
            ext.rprenames$scroll(-1);
        }

        cir.setReturnValue(true);
    }
}
