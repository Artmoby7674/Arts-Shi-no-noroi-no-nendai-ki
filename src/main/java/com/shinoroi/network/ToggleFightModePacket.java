package com.shinoroi.network;

import com.shinoroi.ShinoRoi;
import com.shinoroi.compat.CuriosCompat;
import com.shinoroi.config.ModConfig;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client → Server: player pressed the fight-mode toggle key.
 *
 * <p>When fight mode is toggled ON:
 * <ul>
 *   <li>5-second cooldown check (blocks spam)</li>
 *   <li>Applies a transient ARMOR {@link AttributeModifier} (ADD_VALUE)</li>
 *   <li>Saves and clears main inventory, offhand, armor, and Curios slots</li>
 * </ul>
 *
 * <p>When fight mode is toggled OFF:
 * <ul>
 *   <li>Removes the armor modifier</li>
 *   <li>Restores all stored slots (main, offhand, armor, Curios)</li>
 * </ul>
 */
public record ToggleFightModePacket() implements CustomPacketPayload {

    /** ResourceLocation used as the AttributeModifier ID for the armor bonus. */
    public static final ResourceLocation FIGHT_MODE_ATTR_MOD_ID =
        ResourceLocation.fromNamespaceAndPath(ShinoRoi.MODID, "fight_mode_armor");

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

            // ── 5-second toggle cooldown ──────────────────────────────────────
            long gameTick = player.serverLevel().getGameTime();
            long cooldownTicks = ModConfig.TOGGLE_COOLDOWN_TICKS.get();
            if (gameTick - data.getLastToggleTick() < cooldownTicks) {
                return; // still on cooldown — silently ignore
            }

            boolean entering = !data.isFightModeActive();
            data.setFightModeActive(entering);
            data.setLastToggleTick(gameTick);

            // ── Armor attribute modifier ──────────────────────────────────────
            var armorAttr = player.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.removeModifier(FIGHT_MODE_ATTR_MOD_ID);
                if (entering) {
                    armorAttr.addTransientModifier(new AttributeModifier(
                        FIGHT_MODE_ATTR_MOD_ID,
                        ModConfig.DEFAULT_FIGHT_MODE_ARMOR_BONUS.get(),
                        AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            }

            // ── Inventory management ──────────────────────────────────────────
            if (entering) {
                saveAndClearInventory(player, data);
            } else {
                restoreInventory(player, data);
            }

            player.setData(ModAttachments.PLAYER_DATA.get(), data);
            ModNetwork.syncToClient(player, data);

            ShinoRoi.LOGGER.debug("[ShinoRoi] {} fight mode → {}",
                player.getScoreboardName(), entering);
        });
    }

    /**
     * Saves and clears: main inventory (36), offhand (1), armor (4), and
     * any equipped Curios slots (if Curios API is loaded).
     *
     * Snapshot layout: [main 0..35][offhand 36][armor 37..40][curios 41+]
     */
    private static void saveAndClearInventory(ServerPlayer player, PlayerData data) {
        var inv = player.getInventory();
        List<ItemStack> snapshot = new ArrayList<>();

        // Main inventory (36 slots)
        for (int i = 0; i < inv.items.size(); i++) {
            snapshot.add(inv.items.get(i).copy());
            inv.items.set(i, ItemStack.EMPTY);
        }
        // Offhand (1 slot)
        for (int i = 0; i < inv.offhand.size(); i++) {
            snapshot.add(inv.offhand.get(i).copy());
            inv.offhand.set(i, ItemStack.EMPTY);
        }
        // Armor (4 slots: feet, legs, chest, head)
        for (int i = 0; i < inv.armor.size(); i++) {
            snapshot.add(inv.armor.get(i).copy());
            inv.armor.set(i, ItemStack.EMPTY);
        }
        // Curios (optional mod)
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.saveAndClear(player, snapshot);
        }

        data.storeInventory(snapshot);
        player.inventoryMenu.sendAllDataToRemote();
    }

    /**
     * Restores items previously saved by {@link #saveAndClearInventory} back into
     * the player's live inventory, then clears the stored snapshot.
     *
     * Snapshot layout: [main 0..35][offhand 36][armor 37..40][curios 41+]
     */
    private static void restoreInventory(ServerPlayer player, PlayerData data) {
        List<ItemStack> stored = new ArrayList<>(data.getStoredInventory());
        if (stored.isEmpty()) return;

        var inv = player.getInventory();
        int idx = 0;

        // Main inventory
        for (int i = 0; i < inv.items.size() && idx < stored.size(); i++, idx++) {
            inv.items.set(i, stored.get(idx).copy());
        }
        // Offhand
        for (int i = 0; i < inv.offhand.size() && idx < stored.size(); i++, idx++) {
            inv.offhand.set(i, stored.get(idx).copy());
        }
        // Armor
        for (int i = 0; i < inv.armor.size() && idx < stored.size(); i++, idx++) {
            inv.armor.set(i, stored.get(idx).copy());
        }
        // Curios
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.restore(player, stored, idx);
        }

        data.clearStoredInventory();
        player.inventoryMenu.sendAllDataToRemote();
    }
}
