package com.shinoroi;

import com.mojang.logging.LogUtils;
import com.shinoroi.client.ClientSetup;
import com.shinoroi.client.ClientTickHandler;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.fightmode.FightModeHandler;
import com.shinoroi.network.ModNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entry point for Shi no Noroi.
 *
 * All event bus subscriptions are wired here explicitly rather than via
 * @EventBusSubscriber(bus = Bus.X), which is deprecated since NeoForge 1.21.1.
 *
 * Registration order:
 *  1. AttachmentType registry  (mod bus)
 *  2. Network packets          (mod bus  — ModNetwork)
 *  3. Game events              (game bus — FightModeHandler)
 *  4. Client-only              (mod bus  — ClientSetup)
 *                              (game bus — ClientTickHandler)
 */
@Mod(ShinoRoi.MODID)
public class ShinoRoi {

    public static final String MODID = "shinoroi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShinoRoi(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Attachment registry — must come first so PlayerData is available
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 2. Network packet registration (mod lifecycle bus)
        modEventBus.register(ModNetwork.class);

        // 3. Server-side fight mode enforcement (game event bus)
        NeoForge.EVENT_BUS.register(FightModeHandler.class);

        // 4. Client-only: keybinds + HUD (mod bus) and tick/render handlers (game bus)
        //    The dist check ensures client-only classes are never loaded on a server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientSetup.class);
            NeoForge.EVENT_BUS.register(ClientTickHandler.class);
        }

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[ShinoRoi] Common setup complete.");
    }
}
