package com.shinoroi.hud;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom HUD overlay rendered when fight mode is active.
 *
 * Current layout:
 *   TOP-CENTER  — "[ FIGHT MODE ]" indicator
 *   BOTTOM-LEFT — energy bar + numeric readout
 *
 * Future additions per-technique will slot into this class:
 *   technique list, cooldown pips, rank indicator, domain expansion charge.
 */
public class FightHud {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "fight_hud");

    // Energy bar geometry (relative to bottom-left corner)
    private static final int BAR_WIDTH  = 120;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_X      = 10;
    /** Distance from screen bottom */
    private static final int BAR_Y_BOTTOM_OFFSET = 38;

    // Colours
    private static final int COLOR_BAR_BG    = 0xAA000000;
    private static final int COLOR_BAR_FILL  = 0xFF3399FF;
    private static final int COLOR_BAR_LOW   = 0xFFFF4422; // below 25 %
    private static final int COLOR_LABEL     = 0xFFFFFFFF;
    private static final int COLOR_INDICATOR = 0xFFFF3300;

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // ── Fight mode indicator ──────────────────────────────────────────────
        graphics.drawCenteredString(mc.font, "[ FIGHT MODE ]", sw / 2, 6, COLOR_INDICATOR);

        // ── Energy bar ────────────────────────────────────────────────────────
        int barY = sh - BAR_Y_BOTTOM_OFFSET;

        // Background
        graphics.fill(BAR_X - 1, barY - 1,
                      BAR_X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1,
                      COLOR_BAR_BG);

        // Fill
        float ratio = (data.getMaxEnergy() > 0)
            ? data.getEnergy() / data.getMaxEnergy()
            : 0f;
        int fillWidth = (int) (BAR_WIDTH * ratio);
        if (fillWidth > 0) {
            int fillColor = (ratio <= 0.25f) ? COLOR_BAR_LOW : COLOR_BAR_FILL;
            graphics.fill(BAR_X, barY, BAR_X + fillWidth, barY + BAR_HEIGHT, fillColor);
        }

        // Numeric label above the bar
        String label = String.format("%.0f / %.0f", data.getEnergy(), data.getMaxEnergy());
        graphics.drawString(mc.font, label, BAR_X, barY - 10, COLOR_LABEL, true);

        // ── Technique slots (placeholder) ─────────────────────────────────────
        // Will be populated once techniques are added.
    }
}
