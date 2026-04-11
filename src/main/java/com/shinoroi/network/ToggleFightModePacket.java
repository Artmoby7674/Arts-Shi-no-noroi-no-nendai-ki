package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player pressed the fight-mode toggle key.
 * Server applies the state change and syncs back.
 */
public record ToggleFightModePacket() implements CustomPacketPayload {

    public static final Type<ToggleFightModePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "toggle_fight_mode")
    );

    public static final StreamCodec<FriendlyByteBuf, ToggleFightModePacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> { /* empty payload */ },
            buf -> new ToggleFightModePacket()
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ToggleFightModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
            data.setFightModeActive(!data.isFightModeActive());
            player.setData(ModAttachments.PLAYER_DATA.get(), data);
            ModNetwork.syncToClient(player, data);

            ShinoRoi.LOGGER.debug("[ShinoRoi] {} fight mode → {}",
                player.getScoreboardName(), data.isFightModeActive());
        });
    }
}
