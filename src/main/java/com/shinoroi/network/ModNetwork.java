package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
            SyncPlayerDataPacket.TYPE,
            SyncPlayerDataPacket.STREAM_CODEC,
            SyncPlayerDataPacket::handleOnClient
        );

        registrar.playToServer(
            ToggleFightModePacket.TYPE,
            ToggleFightModePacket.STREAM_CODEC,
            ToggleFightModePacket::handleOnServer
        );
    }

    /** Push a full PlayerData snapshot to the given player's client. */
    public static void syncToClient(ServerPlayer player, PlayerData data) {
        try {
            PacketDistributor.sendToPlayer(player, new SyncPlayerDataPacket(data));
        } catch (Exception e) {
            ShinoRoi.LOGGER.error("[ShinoRoi] Failed to sync player data to {}: {}",
                player.getScoreboardName(), e.getMessage());
        }
    }
}
