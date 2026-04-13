package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client: lightweight ult bar sync.
 *
 * <p>Sent frequently during combat to update the ult bar display without
 * sending the full {@link SyncPlayerDataPacket}.</p>
 */
public record SyncUltBarPacket(float ultBar) implements CustomPacketPayload {

    public static final Type<SyncUltBarPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "sync_ult_bar")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncUltBarPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeFloat(pkt.ultBar()),
            buf -> new SyncUltBarPacket(buf.readFloat())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(SyncUltBarPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
            data.setUltBar(packet.ultBar());
            player.setData(ModAttachments.PLAYER_DATA.get(), data);
        });
    }
}
