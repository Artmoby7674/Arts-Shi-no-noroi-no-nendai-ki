package com.shinoroi.client;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.data.TechniqueDefinition;
import com.shinoroi.network.SkillTreeActionPacket;
import com.shinoroi.registry.MovesetRegistry;
import com.shinoroi.registry.TechniqueRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scrollable skill tree screen showing technique cards for the player's
 * active moveset.
 *
 * <p>Layout:
 * <ul>
 *   <li>Title bar at top with moveset name</li>
 *   <li>Skill points display (bottom-left)</li>
 *   <li>Grid of technique cards, 3 per row, scrollable</li>
 *   <li>Each card: name, type, level, Unlock / Upgrade / Maxed button</li>
 * </ul>
 * </p>
 *
 * <p>Open with the K keybind (handled in {@link ClientTickHandler}).</p>
 */
public class SkillTreeScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int CARD_W       = 100;
    private static final int CARD_H       = 80;
    private static final int CARD_PAD     = 10;
    private static final int CARDS_PER_ROW = 3;
    private static final int TOP_MARGIN   = 40;
    private static final int SCROLL_STEP  = 20;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COLOR_BG            = 0xCC000000;
    private static final int COLOR_CARD_BG       = 0xFF1A1A2E;
    private static final int COLOR_CARD_BORDER   = 0xFF444466;
    private static final int COLOR_CARD_UNLOCKED = 0xFF225522;
    private static final int COLOR_TITLE         = 0xFFFFCC00;
    private static final int COLOR_TEXT          = 0xFFCCCCCC;
    private static final int COLOR_BTN_NORMAL    = 0xFF334466;
    private static final int COLOR_BTN_HOVER     = 0xFF4455AA;
    private static final int COLOR_BTN_DISABLED  = 0xFF333333;
    private static final int COLOR_BTN_TEXT      = 0xFFFFFFFF;
    private static final int COLOR_SP            = 0xFFFFFF44;

    // ── State ─────────────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    private int maxScroll    = 0;
    private final List<TechniqueCard> cards = new ArrayList<>();

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();
        scrollOffset = 0;
        loadCards();
    }

    private void loadCards() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());

        Collection<TechniqueDefinition> allTechniques;
        if (moveset != null) {
            // Show only the moveset's techniques in order
            List<TechniqueDefinition> ordered = new ArrayList<>();
            for (ResourceLocation id : moveset.techniques()) {
                TechniqueDefinition def = TechniqueRegistry.INSTANCE.get(id);
                if (def != null) ordered.add(def);
            }
            allTechniques = ordered;
        } else {
            allTechniques = TechniqueRegistry.INSTANCE.all();
        }

        for (TechniqueDefinition def : allTechniques) {
            cards.add(new TechniqueCard(def));
        }

        // Compute max scroll
        int rows = (int) Math.ceil((double) cards.size() / CARDS_PER_ROW);
        int totalH = rows * (CARD_H + CARD_PAD) + TOP_MARGIN;
        maxScroll = Math.max(0, totalH - height + 20);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render vanilla blur/background FIRST so it stays behind all custom content
        super.render(graphics, mouseX, mouseY, partialTick);

        // Dim overlay on top of the blur
        graphics.fill(0, 0, width, height, COLOR_BG);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());

        // ── Title ─────────────────────────────────────────────────────────────
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        String title = (moveset != null) ? moveset.displayName() + " — Skill Tree" : "Skill Tree";
        graphics.drawCenteredString(font, title, width / 2, 10, COLOR_TITLE);

        // ── Skill points ──────────────────────────────────────────────────────
        graphics.drawString(font, "Pts: " + data.getSkillPoints(), 10, height - 14, COLOR_SP, true);
        graphics.drawString(font, "Rank: " + data.getRank(), 10, height - 24, COLOR_TEXT, true);
        graphics.drawString(font, "[ESC] to close  |  Scroll to browse", width / 2 - 80, height - 14, COLOR_TEXT, false);

        // ── Cards ─────────────────────────────────────────────────────────────
        int startX = (width - (CARDS_PER_ROW * (CARD_W + CARD_PAD) - CARD_PAD)) / 2;
        for (int i = 0; i < cards.size(); i++) {
            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;
            int cardX = startX + col * (CARD_W + CARD_PAD);
            int cardY = TOP_MARGIN + row * (CARD_H + CARD_PAD) - scrollOffset;

            if (cardY + CARD_H < 0 || cardY > height) continue;

            renderCard(graphics, cards.get(i), data, cardX, cardY, mouseX, mouseY);
        }
    }

    private void renderCard(GuiGraphics graphics, TechniqueCard card, PlayerData data,
                             int x, int y, int mouseX, int mouseY) {
        TechniqueDefinition def = card.def;
        boolean unlocked = data.hasTechnique(def.id());
        int level = data.getUpgradeLevel(def.id());

        // Card background
        int bgColor = unlocked ? COLOR_CARD_UNLOCKED : COLOR_CARD_BG;
        graphics.fill(x, y, x + CARD_W, y + CARD_H, bgColor);
        drawBorder(graphics, x, y, CARD_W, CARD_H, COLOR_CARD_BORDER);

        // Name
        String name = def.displayName().length() > 14
            ? def.displayName().substring(0, 14) : def.displayName();
        graphics.drawString(font, name, x + 4, y + 4, COLOR_TITLE, true);

        // Type & attack type
        graphics.drawString(font, def.type().name() + " / " + def.attackType().name(),
            x + 4, y + 16, COLOR_TEXT, false);

        // SP cost
        graphics.drawString(font, "Pts: " + def.skillPointCost(), x + 4, y + 26, COLOR_SP, false);

        // Level indicator
        if (unlocked && def.upgradeLevels() > 0) {
            graphics.drawString(font, "Lv " + level + "/" + def.upgradeLevels(),
                x + 4, y + 36, COLOR_TEXT, false);
        }

        // ── Button ────────────────────────────────────────────────────────────
        int btnX = x + 4;
        int btnY = y + CARD_H - 18;
        int btnW = CARD_W - 8;
        int btnH = 14;

        String btnLabel;
        boolean canClick;

        if (!unlocked) {
            btnLabel = "Unlock";
            canClick = true;
        } else if (def.upgradeLevels() > 0 && level < def.upgradeLevels()) {
            btnLabel = "Upgrade";
            canClick = true;
        } else {
            btnLabel = unlocked ? (def.upgradeLevels() > 0 ? "Maxed" : "Unlocked") : "Unlock";
            canClick = false;
        }

        boolean hover = mouseX >= btnX && mouseX <= btnX + btnW
                     && mouseY >= btnY && mouseY <= btnY + btnH
                     && canClick;

        int btnColor = !canClick ? COLOR_BTN_DISABLED : (hover ? COLOR_BTN_HOVER : COLOR_BTN_NORMAL);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnColor);
        graphics.drawCenteredString(font, btnLabel, btnX + btnW / 2, btnY + 3, COLOR_BTN_TEXT);

        card.btnX = btnX; card.btnY = btnY;
        card.btnW = btnW; card.btnH = btnH;
        card.canClick = canClick;
        card.isUpgrade = unlocked && def.upgradeLevels() > 0 && level < def.upgradeLevels();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        for (TechniqueCard card : cards) {
            if (!card.canClick) continue;
            if (mouseX >= card.btnX && mouseX <= card.btnX + card.btnW
             && mouseY >= card.btnY && mouseY <= card.btnY + card.btnH) {
                PacketDistributor.sendToServer(
                    new SkillTreeActionPacket(card.def.id(), card.isUpgrade));
                // Refresh cards on next render tick (data syncs back from server)
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll,
            scrollOffset - scrollY * SCROLL_STEP));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ── Card wrapper ──────────────────────────────────────────────────────────

    private static class TechniqueCard {
        final TechniqueDefinition def;
        int btnX, btnY, btnW, btnH;
        boolean canClick;
        boolean isUpgrade;

        TechniqueCard(TechniqueDefinition def) {
            this.def = def;
        }
    }
}
