package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.combat.QteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player pressed an arrow key during a QTE session.
 *
 * <p>Direction encoding: 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT.</p>
 * <p>Index is the position in the sequence the player is responding to.</p>
 */
public record QteKeyPacket(int direction, int index) implements CustomPacketPayload {

    public static final Type<QteKeyPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "qte_key")
    );

    public static final StreamCodec<FriendlyByteBuf, QteKeyPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.direction());
                buf.writeVarInt(pkt.index());
            },
            buf -> new QteKeyPacket(buf.readVarInt(), buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(QteKeyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            QteManager.INSTANCE.handleKeyPress(player.getUUID(), packet.direction(), player.server);
        });
    }
}
