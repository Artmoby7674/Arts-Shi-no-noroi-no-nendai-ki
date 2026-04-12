package com.shinoroi.client;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModKeybinds;
import com.shinoroi.hud.FightHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Mod-bus client-only registrations: keybinds and HUD overlay.
 *
 * Vanilla layer suppression (hotbar / selected-item name) is handled in
 * ClientTickHandler via RenderGuiLayerEvent.Pre so we avoid the nested-lambda
 * type-inference issues that wrapLayer introduces.
 */
@EventBusSubscriber(modid = ShinoRoi.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        // Register our fight HUD above the vanilla hotbar slot
        event.registerAbove(VanillaGuiLayers.HOTBAR, FightHud.ID, FightHud::render);
    }
}
