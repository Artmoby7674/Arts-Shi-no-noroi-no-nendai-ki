package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client: notifies a player that a QTE has started and provides
 * the arrow sequence, their role, and the session duration.
 */
public record QteSyncPacket(Role role, int[] sequence, int durationTicks, float baseDamage)
        implements CustomPacketPayload {

    public enum Role { ATTACKER, DEFENDER }

    public static final Type<QteSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "qte_sync")
    );

    public static final StreamCodec<FriendlyByteBuf, QteSyncPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeEnum(pkt.role());
                buf.writeVarInt(pkt.sequence().length);
                for (int v : pkt.sequence()) buf.writeVarInt(v);
                buf.writeVarInt(pkt.durationTicks());
                buf.writeFloat(pkt.baseDamage());
            },
            buf -> {
                Role role = buf.readEnum(Role.class);
                int len = buf.readVarInt();
                int[] seq = new int[len];
                for (int i = 0; i < len; i++) seq[i] = buf.readVarInt();
                int duration = buf.readVarInt();
                float damage = buf.readFloat();
                return new QteSyncPacket(role, seq, duration, damage);
            }
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(QteSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store the active QTE data client-side for QteOverlay rendering
            com.shinoroi.hud.QteOverlay.startQte(packet.role(), packet.sequence(), packet.durationTicks());
        });
    }
}
