package com.shinoroi;

import com.mojang.logging.LogUtils;
import com.shinoroi.core.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Entry point for Shi no Noroi.
 *
 * Initialisation order:
 *  1. AttachmentType registry  — player data is ready before anything runs
 *  2. Network packets          — registered via @EventBusSubscriber in ModNetwork
 *  3. Game events              — registered via @EventBusSubscriber in FightModeHandler
 *  4. Client-only events       — registered via @EventBusSubscriber in ClientSetup /
 *                                ClientTickHandler (Dist.CLIENT guard)
 */
@Mod(ShinoRoi.MODID)
public class ShinoRoi {

    public static final String MODID = "shinoroi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShinoRoi(IEventBus modEventBus, ModContainer modContainer) {
        // Attachment registry must be registered to the mod bus explicitly
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[ShinoRoi] Common setup complete.");
    }
}
