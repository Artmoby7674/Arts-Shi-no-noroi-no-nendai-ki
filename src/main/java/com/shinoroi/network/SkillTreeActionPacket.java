package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import com.shinoroi.data.TechniqueDefinition;
import com.shinoroi.registry.TechniqueRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player clicked unlock or upgrade on a technique card
 * in the skill tree screen.
 *
 * <p>Server validates skill points and applies the change.</p>
 */
public record SkillTreeActionPacket(ResourceLocation techniqueId, boolean isUpgrade)
        implements CustomPacketPayload {

    public static final Type<SkillTreeActionPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "skill_tree_action")
    );

    public static final StreamCodec<FriendlyByteBuf, SkillTreeActionPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeResourceLocation(pkt.techniqueId());
                buf.writeBoolean(pkt.isUpgrade());
            },
            buf -> new SkillTreeActionPacket(buf.readResourceLocation(), buf.readBoolean())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(SkillTreeActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            TechniqueDefinition def = TechniqueRegistry.INSTANCE.get(packet.techniqueId());
            if (def == null) return;

            PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());

            if (packet.isUpgrade()) {
                data.upgradeTechnique(packet.techniqueId(), def.upgradeLevels(), def.skillPointCost());
            } else {
                // Unlock
                if (!data.hasTechnique(packet.techniqueId())) {
                    if (data.spendSkillPoints(def.skillPointCost())) {
                        data.unlockTechnique(packet.techniqueId());
                    }
                }
            }

            player.setData(ModAttachments.PLAYER_DATA.get(), data);
            ModNetwork.syncToClient(player, data);
        });
    }
}
