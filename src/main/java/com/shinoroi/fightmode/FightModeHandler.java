package com.shinoroi.fightmode;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import com.shinoroi.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingTickEvent;
import net.neoforged.neoforge.event.entity.player.EntityItemPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Server-side game-event handler for fight mode restrictions and energy regen.
 *
 * Fight mode rules (enforced here on the server):
 *  - No block placement
 *  - No right-click item use (food, potions, etc.)
 *  - No item pickup
 *  - Block breaking only with bare hand (tool in hand → cancel)
 *  - Passive energy regeneration (slower while fighting, faster while idle)
 */
@EventBusSubscriber(modid = ShinoRoi.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FightModeHandler {

    /** Energy regen per tick while fight mode is ACTIVE  (0.2 / tick = 4 / sec) */
    public static final float REGEN_ACTIVE  = 0.2f;
    /** Energy regen per tick while fight mode is INACTIVE (0.5 / tick = 10 / sec) */
    public static final float REGEN_PASSIVE = 0.5f;
    /** Ticks between energy-sync packets during passive regen */
    private static final int SYNC_INTERVAL = 20;

    // ── Energy regen ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());

        if (data.getEnergy() < data.getMaxEnergy()) {
            float regen = data.isFightModeActive() ? REGEN_ACTIVE : REGEN_PASSIVE;
            data.setEnergy(data.getEnergy() + regen);
            // Sync energy every SYNC_INTERVAL ticks to avoid packet spam
            if (player.tickCount % SYNC_INTERVAL == 0) {
                ModNetwork.syncToClient(player, data);
            }
        }
    }

    // ── Block placement ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isFightMode(player)) return;

        // Block ALL right-click block interactions in fight mode
        // (prevents placing blocks, opening chests, activating doors, etc.)
        event.setCanceled(true);
    }

    // ── Item use ─────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isFightMode(player)) return;

        // Block item use (food, potions, bows, etc.)
        event.setCanceled(true);
    }

    // ── Item pickup ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (!isFightMode(player)) return;

        event.setCanceled(true);
    }

    // ── Block breaking enforcement (server) ───────────────────────────────────

    /**
     * Prevents the server from actually breaking a block when the player has
     * a tool in their main hand during fight mode.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!isFightMode(player)) return;

        if (!player.getMainHandItem().isEmpty()) {
            event.setCanceled(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isFightMode(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA.get()).isFightModeActive();
    }
}
