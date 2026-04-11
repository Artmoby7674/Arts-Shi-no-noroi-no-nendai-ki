package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client: pushes the full PlayerData snapshot so the client
 * can display the HUD and apply camera state correctly.
 */
public record SyncPlayerDataPacket(CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<SyncPlayerDataPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "sync_player_data")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerDataPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeNbt(pkt.nbt()),
            buf -> {
                CompoundTag tag = buf.readNbt();
                return new SyncPlayerDataPacket(tag != null ? tag : new CompoundTag());
            }
        );

    /** Convenience constructor: serialises a PlayerData directly. */
    public SyncPlayerDataPacket(PlayerData data) {
        this((CompoundTag) PlayerData.CODEC
            .encodeStart(NbtOps.INSTANCE, data)
            .getOrThrow());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(SyncPlayerDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            PlayerData.CODEC
                .decode(NbtOps.INSTANCE, packet.nbt())
                .result()
                .ifPresent(pair -> player.setData(ModAttachments.PLAYER_DATA.get(), pair.getFirst()));
        });
    }
}
