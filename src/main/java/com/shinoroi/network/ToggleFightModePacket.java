package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.config.ModConfig;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.registry.MovesetRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server: player pressed the fight-mode toggle key.
 *
 * <p>When fight mode is toggled ON: applies a transient MAX_HEALTH
 * {@link AttributeModifier} (ADD_VALUE) and heals the player for the bonus.</p>
 *
 * <p>When fight mode is toggled OFF: removes the modifier and clamps
 * current health to the new (lower) maximum.</p>
 */
public record ToggleFightModePacket() implements CustomPacketPayload {

    /** ResourceLocation used as the AttributeModifier ID. */
    public static final ResourceLocation FIGHT_MODE_HP_MOD_ID =
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "fight_mode_hp");

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
            boolean entering = !data.isFightModeActive();

            data.setFightModeActive(entering);

            var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                // Always remove any existing modifier first to avoid stacking
                maxHealthAttr.removeModifier(FIGHT_MODE_HP_MOD_ID);

                if (entering) {
                    double hpBonus = resolveFightModeHpBonus(data);
                    maxHealthAttr.addTransientModifier(new AttributeModifier(
                        FIGHT_MODE_HP_MOD_ID,
                        hpBonus,
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                    // Heal the player for the bonus amount so they feel the HP instantly
                    player.heal((float) hpBonus);
                } else {
                    // Clamp current health to the new (lower) maximum
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            }

            player.setData(ModAttachments.PLAYER_DATA.get(), data);
            ModNetwork.syncToClient(player, data);

            ShinoRoi.LOGGER.debug("[ShinoRoi] {} fight mode → {}",
                player.getScoreboardName(), entering);
        });
    }

    /**
     * Returns the HP bonus for the player's active moveset, falling back to
     * the config default if no moveset is equipped or the moveset uses -1.
     */
    private static double resolveFightModeHpBonus(PlayerData data) {
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        if (moveset != null && moveset.fightModeHpBonus() >= 0) {
            return moveset.fightModeHpBonus();
        }
        return ModConfig.DEFAULT_FIGHT_MODE_HP_BONUS.get();
    }
}
