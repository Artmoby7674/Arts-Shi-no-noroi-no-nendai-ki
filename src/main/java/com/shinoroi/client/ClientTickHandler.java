package com.shinoroi.client;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.core.ModKeybinds;
import com.shinoroi.data.PlayerData;
import com.shinoroi.network.ToggleFightModePacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Game-bus client-only tick handler.
 *
 * Responsibilities:
 *  - Detect fight-mode toggle keybind and send packet to server
 *  - Watch for fight-mode state changes (from synced data) and
 *    switch the camera between first-person and third-person right-shoulder
 *  - Block break speed reduction when holding a tool in fight mode (visual)
 *  - Consume technique keys (wired up when techniques are added)
 */
@EventBusSubscriber(modid = ShinoRoi.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientTickHandler {

    /** Camera state tracked across ticks to detect transitions */
    private static boolean wasFightModeActive = false;
    private static CameraType cameraBeforeFightMode = null;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || event.getEntity() != player) return;

        // ── 1. Toggle key ─────────────────────────────────────────────────────
        while (ModKeybinds.TOGGLE_FIGHT_MODE.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleFightModePacket());
        }

        // Technique keys — consumed now, wired to techniques when added
        while (ModKeybinds.TECHNIQUE_1.consumeClick()) { /* TODO */ }
        while (ModKeybinds.TECHNIQUE_2.consumeClick()) { /* TODO */ }
        while (ModKeybinds.TECHNIQUE_3.consumeClick()) { /* TODO */ }
        while (ModKeybinds.DOMAIN_EXPANSION.consumeClick()) { /* TODO */ }

        // ── 2. Camera switching on fight-mode transitions ─────────────────────
        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        boolean fightModeNow = data.isFightModeActive();

        if (fightModeNow && !wasFightModeActive) {
            // Entering fight mode → save camera and switch to third-person back
            // The MixinCamera will apply the right-shoulder offset automatically.
            cameraBeforeFightMode = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            ShinoRoi.LOGGER.debug("[ShinoRoi] Fight mode ON — camera → THIRD_PERSON_BACK");

        } else if (!fightModeNow && wasFightModeActive) {
            // Leaving fight mode → restore previous camera
            CameraType restore = (cameraBeforeFightMode != null)
                ? cameraBeforeFightMode
                : CameraType.FIRST_PERSON;
            mc.options.setCameraType(restore);
            cameraBeforeFightMode = null;
            ShinoRoi.LOGGER.debug("[ShinoRoi] Fight mode OFF — camera restored to {}", restore);
        }

        // While in fight mode, prevent the player from manually cycling away
        // from third-person back (e.g. pressing F5).
        if (fightModeNow && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        wasFightModeActive = fightModeNow;
    }

    // ── Suppress vanilla layers while fight mode is active ────────────────────

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

    // ── Break-speed modifier (client-side, for visual feedback) ───────────────

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerData data = event.getEntity().getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        if (!event.getEntity().getMainHandItem().isEmpty()) {
            // Tool in hand during fight mode — cancel break entirely
            event.setCanceled(true);
        } else {
            // Bare-hand breaking is allowed but significantly slowed
            event.setNewSpeed(event.getOriginalSpeed() * 0.15f);
        }
    }
}
