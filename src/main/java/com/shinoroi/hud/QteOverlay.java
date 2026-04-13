package com.shinoroi.hud;

import com.shinoroi.ShinoRoi;
import com.shinoroi.network.QteKeyPacket;
import com.shinoroi.network.QteSyncPacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * HUD overlay rendered during an active QTE session.
 *
 * Layout (rendered above FightHud):
 *   CENTER-TOP   — role label ("ATTACKER" / "DEFENDER")
 *   CENTER       — current arrow prompt (large arrow character)
 *   BELOW ARROW  — sequence progress (● for correct/done, ○ for upcoming)
 *   BOTTOM-LEFT  — time bar (drains left-to-right over durationTicks)
 *   CENTER-LOWER — balance gauge (-/+ with needle)
 *
 * Static state is written by {@link QteSyncPacket#handleOnClient} and read
 * by the render method each frame.
 */
public class QteOverlay {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "qte_overlay");

    // ── Client-side session state ─────────────────────────────────────────────

    private static QteSyncPacket.Role activeRole;
    private static int[] activeSequence;
    private static int durationTicks;
    private static long sessionStartTick;
    private static int currentIndex;

    /** Called from {@link QteSyncPacket#handleOnClient} on the render thread. */
    public static void startQte(QteSyncPacket.Role role, int[] sequence, int duration) {
        activeRole = role;
        activeSequence = sequence.clone();
        durationTicks = duration;
        sessionStartTick = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.getGameTime()
            : 0;
        currentIndex = 0;
    }

    /** Called from {@link com.shinoroi.client.ClientTickHandler} when an arrow key is consumed. */
    public static void advanceIndex(int direction) {
        if (activeSequence == null) return;
        if (currentIndex < activeSequence.length) {
            // Send key press to server
            PacketDistributor.sendToServer(new QteKeyPacket(direction, currentIndex));
            currentIndex++;
        }
        if (currentIndex >= activeSequence.length) {
            // All keys pressed — don't clear yet; wait for server resolution
        }
    }

    /** Called when the server resolves or a new QTE starts, to clear local state. */
    public static void clear() {
        activeRole = null;
        activeSequence = null;
    }

    public static boolean isActive() {
        return activeRole != null && activeSequence != null;
    }

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COLOR_BG           = 0xBB000000;
    private static final int COLOR_ATTACKER     = 0xFFFF4400;
    private static final int COLOR_DEFENDER     = 0xFF3399FF;
    private static final int COLOR_ARROW        = 0xFFFFFFFF;
    private static final int COLOR_DONE         = 0xFF44FF44;
    private static final int COLOR_UPCOMING     = 0xFF888888;
    private static final int COLOR_TIMEBAR_BG   = 0xFF333333;
    private static final int COLOR_TIMEBAR_FILL = 0xFFFFCC00;
    private static final int COLOR_BALANCE_MARK = 0xFFFFFFFF;

    // ── Arrow characters (Unicode directional arrows) ─────────────────────────
    private static final String[] ARROW_CHARS = { "↑", "↓", "←", "→" };

    // ── Render ────────────────────────────────────────────────────────────────

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
        long elapsed = currentTick - sessionStartTick;

        if (elapsed > durationTicks) {
            clear();
            return;
        }

        int centerX = sw / 2;
        int baseY = sh / 2 - 60;

        // ── Panel background ─────────────────────────────────────────────────
        graphics.fill(centerX - 80, baseY - 5, centerX + 80, baseY + 90, COLOR_BG);

        // ── Role label ────────────────────────────────────────────────────────
        int roleColor = (activeRole == QteSyncPacket.Role.ATTACKER)
            ? COLOR_ATTACKER : COLOR_DEFENDER;
        String roleLabel = (activeRole == QteSyncPacket.Role.ATTACKER)
            ? "ATTACKER" : "DEFENDER";
        graphics.drawCenteredString(mc.font, roleLabel, centerX, baseY, roleColor);

        // ── Current arrow prompt ──────────────────────────────────────────────
        if (currentIndex < activeSequence.length) {
            String arrow = ARROW_CHARS[activeSequence[currentIndex]];
            // Draw large (2× scale via matrix push)
            graphics.pose().pushPose();
            graphics.pose().scale(2f, 2f, 1f);
            graphics.drawCenteredString(mc.font, arrow,
                centerX / 2, (baseY + 14) / 2, COLOR_ARROW);
            graphics.pose().popPose();
        }

        // ── Sequence progress dots ────────────────────────────────────────────
        int dotY = baseY + 44;
        int dotSpacing = 14;
        int dotsStartX = centerX - (activeSequence.length * dotSpacing) / 2;
        for (int i = 0; i < activeSequence.length; i++) {
            int dotX = dotsStartX + i * dotSpacing;
            String dot = (i < currentIndex) ? "●" : "○";
            int dotColor = (i < currentIndex) ? COLOR_DONE : COLOR_UPCOMING;
            graphics.drawCenteredString(mc.font, dot, dotX, dotY, dotColor);
        }

        // ── Time bar ──────────────────────────────────────────────────────────
        int barY = baseY + 58;
        int barW = 140;
        int barH = 5;
        int barX = centerX - barW / 2;
        float timeRatio = 1f - (float) elapsed / durationTicks;

        graphics.fill(barX, barY, barX + barW, barY + barH, COLOR_TIMEBAR_BG);
        int fillW = (int) (barW * timeRatio);
        if (fillW > 0) {
            graphics.fill(barX, barY, barX + fillW, barY + barH, COLOR_TIMEBAR_FILL);
        }

        // ── Instruction ───────────────────────────────────────────────────────
        graphics.drawCenteredString(mc.font, "Press arrow key to score!",
            centerX, baseY + 68, 0xFFCCCCCC);
    }
}
