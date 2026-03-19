package com.hiword9.rprenames.client.mixin;

import java.util.List;

public interface RenamePanelScreenExt {
    boolean rprenames$refreshPanelState();
    boolean rprenames$isInsidePanel(double mouseX, double mouseY);
    int rprenames$getHoveredRow(double mouseX, double mouseY);
    List<String> rprenames$getVisibleEntries();
    boolean rprenames$canScroll();
    void rprenames$scroll(int amount);
}
