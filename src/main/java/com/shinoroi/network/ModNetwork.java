package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.data.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ShinoRoi.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Server → Client
        registrar.playToClient(
            SyncPlayerDataPacket.TYPE,
            SyncPlayerDataPacket.STREAM_CODEC,
            SyncPlayerDataPacket::handleOnClient
        );
        registrar.playToClient(
            SyncUltBarPacket.TYPE,
            SyncUltBarPacket.STREAM_CODEC,
            SyncUltBarPacket::handleOnClient
        );
        registrar.playToClient(
            QteSyncPacket.TYPE,
            QteSyncPacket.STREAM_CODEC,
            QteSyncPacket::handleOnClient
        );

        // Client → Server
        registrar.playToServer(
            ToggleFightModePacket.TYPE,
            ToggleFightModePacket.STREAM_CODEC,
            ToggleFightModePacket::handleOnServer
        );
        registrar.playToServer(
            TechniqueActivatePacket.TYPE,
            TechniqueActivatePacket.STREAM_CODEC,
            TechniqueActivatePacket::handleOnServer
        );
        registrar.playToServer(
            SelectSlotPacket.TYPE,
            SelectSlotPacket.STREAM_CODEC,
            SelectSlotPacket::handleOnServer
        );
        registrar.playToServer(
            QteKeyPacket.TYPE,
            QteKeyPacket.STREAM_CODEC,
            QteKeyPacket::handleOnServer
        );
        registrar.playToServer(
            SkillTreeActionPacket.TYPE,
            SkillTreeActionPacket.STREAM_CODEC,
            SkillTreeActionPacket::handleOnServer
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

    /** Lightweight ult bar sync. */
    public static void syncUltBar(ServerPlayer player, float ultBar) {
        try {
            PacketDistributor.sendToPlayer(player, new SyncUltBarPacket(ultBar));
        } catch (Exception e) {
            ShinoRoi.LOGGER.error("[ShinoRoi] Failed to sync ult bar to {}: {}",
                player.getScoreboardName(), e.getMessage());
        }
    }

    /** Send a QTE session start notification to a specific player. */
    public static void sendQteSync(ServerPlayer player, QteSyncPacket packet) {
        try {
            PacketDistributor.sendToPlayer(player, packet);
        } catch (Exception e) {
            ShinoRoi.LOGGER.error("[ShinoRoi] Failed to send QTE sync to {}: {}",
                player.getScoreboardName(), e.getMessage());
        }
    }
}
