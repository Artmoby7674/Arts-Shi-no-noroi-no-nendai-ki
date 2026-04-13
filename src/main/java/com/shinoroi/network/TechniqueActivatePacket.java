package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.combat.TechniqueExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player activated the technique in the given hotbar slot.
 *
 * <p>Triggered by left-click interception in {@link com.shinoroi.client.ClientTickHandler}.</p>
 */
public record TechniqueActivatePacket(int slot) implements CustomPacketPayload {

    public static final Type<TechniqueActivatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "technique_activate")
    );

    public static final StreamCodec<FriendlyByteBuf, TechniqueActivatePacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeVarInt(pkt.slot()),
            buf -> new TechniqueActivatePacket(buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(TechniqueActivatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TechniqueExecutor.execute(player, packet.slot());
        });
    }
}
