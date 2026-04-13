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
 * Client → Server: player scrolled to a new technique hotbar slot.
 *
 * <p>Sent from {@link com.shinoroi.client.ClientTickHandler} scroll handler.</p>
 */
public record SelectSlotPacket(int slot) implements CustomPacketPayload {

    public static final Type<SelectSlotPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "select_slot")
    );

    public static final StreamCodec<FriendlyByteBuf, SelectSlotPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeVarInt(pkt.slot()),
            buf -> new SelectSlotPacket(buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(SelectSlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
            data.setSelectedSlot(packet.slot());
            player.setData(ModAttachments.PLAYER_DATA.get(), data);
            // No sync needed — selectedSlot is authoritative server-side and echoed via SyncPlayerData
        });
    }
}
