package com.shinoroi.client;

import com.shinoroi.core.ModKeybinds;
import com.shinoroi.hud.FightHud;
import com.shinoroi.hud.QteOverlay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mod-bus client-only registrations: keybinds and HUD overlays.
 * Registered manually in ShinoRoi constructor (avoids deprecated EventBusSubscriber.Bus).
 *
 * Layer render order (bottom-to-top of draw stack above hotbar position):
 *   1. FightHud    — ult bar, technique hotbar, fight mode indicator
 *   2. QteOverlay  — arrow prompts, time bar (only during active QTE)
 */
public class ClientSetup {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.TOGGLE_FIGHT_MODE);
        event.register(ModKeybinds.OPEN_SKILL_TREE);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // FightHud renders above the vanilla hotbar position
        event.registerAbove(VanillaGuiLayers.HOTBAR, FightHud.ID, FightHud::render);
        // QteOverlay renders above FightHud so the prompt is always on top
        event.registerAbove(FightHud.ID, QteOverlay.ID, QteOverlay::render);
    }
}
