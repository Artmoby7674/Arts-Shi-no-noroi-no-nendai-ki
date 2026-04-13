package com.shinoroi.hud;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.data.TechniqueDefinition;
import com.shinoroi.registry.MovesetRegistry;
import com.shinoroi.registry.TechniqueRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Custom HUD overlay rendered when fight mode is active.
 *
 * Layout:
 *   TOP-CENTER      -- "[ FIGHT MODE ]" indicator
 *   BOTTOM-CENTER   -- technique hotbar (up to 9 slots, scroll to select, cooldown overlay)
 *   BOTTOM-RIGHT    -- ult bar (vertical, fills bottom-to-top)
 */
public class FightHud {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "fight_hud");

    // -- Colour palette -------------------------------------------------------
    private static final int COLOR_INDICATOR    = 0xFFFF3300;
    private static final int COLOR_LABEL        = 0xFFFFFFFF;
    private static final int COLOR_SLOT_BG      = 0xAA000000;
    private static final int COLOR_SLOT_BORDER  = 0xFF666666;
    private static final int COLOR_SLOT_SEL     = 0xFFFFCC00;
    private static final int COLOR_COOLDOWN     = 0xAA000000;
    private static final int COLOR_ULT_BG       = 0xAA000000;
    private static final int COLOR_ULT_FILL     = 0xFFAA00FF;
    private static final int COLOR_ULT_FULL     = 0xFFFF00FF;
    private static final int COLOR_ULT_LABEL    = 0xFFFFFFFF;

    // -- Hotbar geometry ------------------------------------------------------
    private static final int SLOT_SIZE          = 20;
    private static final int SLOT_PAD           = 2;
    private static final int HOTBAR_Y_OFFSET    = 48;

    // -- Ult bar geometry (bottom-right corner) --------------------------------
    private static final int ULT_BAR_WIDTH      = 10;
    private static final int ULT_BAR_HEIGHT     = 80;
    private static final int ULT_BAR_X_OFFSET   = 16;
    private static final int ULT_BAR_Y_OFFSET   = 50;

    // -- Render ---------------------------------------------------------------

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        renderIndicator(graphics, mc, sw);
        renderTechniqueHotbar(graphics, mc, sw, sh, data, player);
        renderUltBar(graphics, sw, sh, data);
    }

    // -- Indicator ------------------------------------------------------------

    private static void renderIndicator(GuiGraphics graphics, Minecraft mc, int sw) {
        graphics.drawCenteredString(mc.font, "[ FIGHT MODE ]", sw / 2, 6, COLOR_INDICATOR);
    }

    // -- Technique hotbar -----------------------------------------------------

    private static void renderTechniqueHotbar(GuiGraphics graphics, Minecraft mc,
                                               int sw, int sh, PlayerData data,
                                               LocalPlayer player) {
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        if (moveset == null || moveset.techniques().isEmpty()) return;

        List<ResourceLocation> techniqueIds = moveset.techniques();
        int count = Math.min(techniqueIds.size(), 9);
        int selected = data.getSelectedSlot();
        long gameTick = player.level().getGameTime();

        int totalWidth = count * (SLOT_SIZE + SLOT_PAD) - SLOT_PAD;
        int startX = (sw - totalWidth) / 2;
        int slotY = sh - HOTBAR_Y_OFFSET;

        for (int i = 0; i < count; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_PAD);
            ResourceLocation techId = techniqueIds.get(i);
            boolean isSelected = (i == selected);
            boolean isUnlocked = data.hasTechnique(techId);
            TechniqueDefinition def = TechniqueRegistry.INSTANCE.get(techId);

            // Slot background
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COLOR_SLOT_BG);

            // Slot border -- gold if selected, grey otherwise
            int borderColor = isSelected ? COLOR_SLOT_SEL : COLOR_SLOT_BORDER;
            drawBorder(graphics, slotX, slotY, SLOT_SIZE, SLOT_SIZE, borderColor);

            // Slot number (1-based)
            graphics.drawString(mc.font, String.valueOf(i + 1), slotX + 2, slotY + 2, COLOR_LABEL, true);

            if (!isUnlocked || def == null) {
                graphics.drawCenteredString(mc.font, "?",
                    slotX + SLOT_SIZE / 2, slotY + SLOT_SIZE / 2 - 4, 0xFF666666);
                continue;
            }

            // Abbreviated name (first 3 chars)
            String abbrev = def.displayName().length() > 3
                ? def.displayName().substring(0, 3)
                : def.displayName();
            graphics.drawCenteredString(mc.font, abbrev,
                slotX + SLOT_SIZE / 2, slotY + SLOT_SIZE / 2 - 3, COLOR_LABEL);

            // Cooldown overlay
            long remaining = data.getCooldownRemainingTicks(techId, gameTick);
            if (remaining > 0 && def.cooldownTicks() > 0) {
                float cdRatio = (float) remaining / def.effectiveCooldown(data.getUpgradeLevel(techId));
                int overlayH = (int) (SLOT_SIZE * cdRatio);
                graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + overlayH, COLOR_COOLDOWN);

                String cdText = String.format("%.1f", remaining / 20f);
                graphics.drawCenteredString(mc.font, cdText,
                    slotX + SLOT_SIZE / 2, slotY + SLOT_SIZE / 2 - 3, COLOR_LABEL);
            }
        }
    }

    // -- Ult bar --------------------------------------------------------------

    private static void renderUltBar(GuiGraphics graphics, int sw, int sh, PlayerData data) {
        int barX = sw - ULT_BAR_X_OFFSET - ULT_BAR_WIDTH;
        int barBottomY = sh - ULT_BAR_Y_OFFSET;
        int barTopY = barBottomY - ULT_BAR_HEIGHT;

        // Background
        graphics.fill(barX - 1, barTopY - 1,
                      barX + ULT_BAR_WIDTH + 1, barBottomY + 1,
                      COLOR_ULT_BG);

        // Fill (bottom-to-top)
        float ratio = data.getUltBar() / 100f;
        if (ratio > 0f) {
            int fillH = (int) (ULT_BAR_HEIGHT * ratio);
            int fillY = barBottomY - fillH;
            int fillColor = (ratio >= 1f) ? COLOR_ULT_FULL : COLOR_ULT_FILL;
            graphics.fill(barX, fillY, barX + ULT_BAR_WIDTH, barBottomY, fillColor);
        }

        if (ratio >= 1f) {
            graphics.drawCenteredString(
                Minecraft.getInstance().font, "ULT",
                barX + ULT_BAR_WIDTH / 2, barTopY - 10, COLOR_ULT_FULL);
        }

        // Percentage label below
        String pct = String.format("%d%%", (int) (ratio * 100));
        graphics.drawCenteredString(
            Minecraft.getInstance().font, pct,
            barX + ULT_BAR_WIDTH / 2, barBottomY + 2, COLOR_ULT_LABEL);
    }

    // -- Utility --------------------------------------------------------------

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
