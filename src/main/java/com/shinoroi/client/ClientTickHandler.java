package com.shinoroi.client;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.core.ModKeybinds;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.hud.QteOverlay;
import com.shinoroi.network.SelectSlotPacket;
import com.shinoroi.network.TechniqueActivatePacket;
import com.shinoroi.network.ToggleFightModePacket;
import com.shinoroi.registry.MovesetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Game-bus client-only tick and input handler.
 *
 * Responsibilities:
 *  - Detect fight-mode toggle keybind -> send ToggleFightModePacket
 *  - Scroll wheel interception -> scroll through technique hotbar slots
 *  - Left-click interception -> activate selected technique slot
 *  - Arrow key interception -> forward QTE key press to QteOverlay / server
 *  - Skill tree keybind -> open SkillTreeScreen
 *  - Block render layers (hotbar / selected item name) while fight mode active
 *  - Break-speed modifier (visual)
 *
 * Camera is handled by FightCameraPlugin (Free Camera API 3.2.0).
 */
public class ClientTickHandler {

    // -- Player tick ----------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || event.getEntity() != player) return;

        // 1. Fight-mode toggle
        while (ModKeybinds.TOGGLE_FIGHT_MODE.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFightModePacket());
        }

        // 2. Skill tree screen
        while (ModKeybinds.OPEN_SKILL_TREE.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new SkillTreeScreen());
            }
        }

        // 3. Left-click -> activate technique (fight mode only)
        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (data.isFightModeActive() && mc.screen == null) {
            while (mc.options.keyAttack.consumeClick()) {
                PacketDistributor.sendToServer(
                    new TechniqueActivatePacket(data.getSelectedSlot()));
            }
        }
    }

    // -- Scroll wheel -> technique slot selection -----------------------------

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        event.setCanceled(true); // suppress vanilla hotbar scroll

        int maxSlot = resolveMaxSlot(data);
        if (maxSlot <= 0) return;

        int current = data.getSelectedSlot();
        int delta = (event.getScrollDeltaY() < 0) ? 1 : -1;
        int next = ((current + delta) % (maxSlot + 1) + (maxSlot + 1)) % (maxSlot + 1);

        data.setSelectedSlot(next);
        player.setData(ModAttachments.PLAYER_DATA.get(), data);
        PacketDistributor.sendToServer(new SelectSlotPacket(next));
    }

    // -- Arrow key input -> QTE -----------------------------------------------

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!QteOverlay.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        int direction = -1;
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP    -> direction = 0;
            case GLFW.GLFW_KEY_DOWN  -> direction = 1;
            case GLFW.GLFW_KEY_LEFT  -> direction = 2;
            case GLFW.GLFW_KEY_RIGHT -> direction = 3;
            default -> { return; }
        }

        QteOverlay.advanceIndex(direction);
    }

    // -- Suppress vanilla GUI layers ------------------------------------------

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        if (event.getName().equals(VanillaGuiLayers.HOTBAR)
                || event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            event.setCanceled(true);
        }
    }

    // -- Break-speed modifier -------------------------------------------------

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerData data = event.getEntity().getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        if (!event.getEntity().getMainHandItem().isEmpty()) {
            event.setCanceled(true);
        } else {
            event.setNewSpeed(event.getOriginalSpeed() * 0.15f);
        }
    }

    // -- Helpers --------------------------------------------------------------

    private static int resolveMaxSlot(PlayerData data) {
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        if (moveset == null || moveset.techniques().isEmpty()) return 0;
        return Math.min(moveset.techniques().size(), 9) - 1;
    }
}
