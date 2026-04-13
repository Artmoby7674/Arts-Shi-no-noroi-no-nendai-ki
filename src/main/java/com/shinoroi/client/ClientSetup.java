package com.shinoroi.client;

import com.shinoroi.core.ModKeybinds;
import com.shinoroi.hud.FightHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mod-bus client-only registrations: keybinds and HUD overlay.
 * Registered manually in ShinoRoi constructor (avoids deprecated EventBusSubscriber.Bus).
 *
 * Vanilla layer suppression (hotbar / selected-item name) is handled in
 * ClientTickHandler via RenderGuiLayerEvent.Pre.
 */
public class ClientSetup {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.TOGGLE_FIGHT_MODE);
        event.register(ModKeybinds.TECHNIQUE_1);
        event.register(ModKeybinds.TECHNIQUE_2);
        event.register(ModKeybinds.TECHNIQUE_3);
        event.register(ModKeybinds.DOMAIN_EXPANSION);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, FightHud.ID, FightHud::render);
    }
}
