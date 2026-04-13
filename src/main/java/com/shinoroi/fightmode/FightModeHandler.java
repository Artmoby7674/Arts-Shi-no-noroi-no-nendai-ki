package com.shinoroi.fightmode;

import com.shinoroi.combat.QteManager;
import com.shinoroi.config.ModConfig;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.network.ModNetwork;
import com.shinoroi.registry.MovesetRegistry;
import com.shinoroi.registry.TechniqueRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Server-side game-event handler for fight mode restrictions, ult bar charging,
 * and QTE tick management.
 *
 * Fight mode rules (enforced here on the server):
 *  - No block placement
 *  - No right-click item use (food, potions, etc.)
 *  - Block breaking only with bare hand (tool in hand -> cancel)
 *  - Ult bar charges on damage dealt (configurable rate x moveset multiplier)
 *  - QteManager ticked every overworld server tick
 */
public class FightModeHandler {

    /** Ticks between ult bar sync packets (lightweight path). */
    private static final int ULT_SYNC_INTERVAL = 20;

    // -- Ult bar charging -----------------------------------------------------

    /**
     * Charges the ult bar when a player in fight mode deals damage to any
     * living entity (players or mobs).
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        PlayerData data = attacker.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        float baseCharge = (float) (event.getNewDamage()
            * ModConfig.ULT_CHARGE_PER_DAMAGE.get());

        // Apply moveset multiplier if available
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        float charge = moveset != null
            ? baseCharge * (float) moveset.ultBarChargeRate()
            : baseCharge;

        data.addUltCharge(charge);
        attacker.setData(ModAttachments.PLAYER_DATA.get(), data);

        // Lightweight sync on every hit (ult bar updates feel responsive)
        ModNetwork.syncUltBar(attacker, data.getUltBar());
    }

    // -- QTE manager tick -----------------------------------------------------

    /**
     * Ticks the QteManager every server tick (overworld only to avoid double-ticking
     * in multi-dimension setups).
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(ServerLevel.OVERWORLD)) return;
        QteManager.INSTANCE.tick(level.getServer());
    }

    // -- Block placement ------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isFightMode(player)) return;
        event.setCanceled(true);
    }

    // -- Item use -------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isFightMode(player)) return;
        event.setCanceled(true);
    }

    // -- Block breaking enforcement (server) ----------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!isFightMode(player)) return;
        if (!player.getMainHandItem().isEmpty()) {
            event.setCanceled(true);
        }
    }

    // -- Reload listeners -----------------------------------------------------

    /**
     * Registers TechniqueRegistry and MovesetRegistry as datapack reload listeners.
     * Called once when the server sets up its resource reload pipeline.
     */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(TechniqueRegistry.INSTANCE);
        event.addListener(MovesetRegistry.INSTANCE);
    }

    // -- Helpers --------------------------------------------------------------

    private static boolean isFightMode(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA.get()).isFightModeActive();
    }
}
