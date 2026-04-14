package com.shinoroi.combat;

import com.shinoroi.ShinoRoi;
import com.shinoroi.config.ModConfig;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import com.shinoroi.network.ModNetwork;
import com.shinoroi.network.QteSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * Server-side singleton that manages all active QTE sessions.
 *
 * <p>Call {@link #tick(MinecraftServer)} every server tick from a
 * {@code LevelTickEvent.Post} handler (only for the overworld).</p>
 *
 * <p>Sessions are started by {@link TechniqueExecutor} when a technique hits
 * another player. Mobs are damaged immediately without QTE.</p>
 */
public final class QteManager {

    public static final QteManager INSTANCE = new QteManager();

    private final Map<UUID, QteSession> activeSessionsByAttacker = new HashMap<>();

    private QteManager() {}

    // ── Session management ────────────────────────────────────────────────────

    /**
     * Starts a new QTE session between attacker and defender.
     * If the attacker already has an active session, it is resolved immediately
     * before starting the new one.
     */
    public void startQte(ServerPlayer attacker, ServerPlayer defender,
                          ResourceLocation techniqueId, float baseDamage,
                          MinecraftServer server) {
        UUID attackerId = attacker.getUUID();

        // Resolve any existing session for this attacker
        QteSession existing = activeSessionsByAttacker.get(attackerId);
        if (existing != null && !existing.isResolved()) {
            resolveSession(existing, server);
        }

        int length = ModConfig.QTE_SEQUENCE_LENGTH.get();
        int duration = ModConfig.QTE_DURATION_TICKS.get();
        int[] sequence = generateSequence(length);
        long currentTick = server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD) != null
            ? server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD).getGameTime()
            : server.getTickCount();

        QteSession session = new QteSession(
            attacker.getUUID(), defender.getUUID(),
            techniqueId, sequence, currentTick, duration, baseDamage);

        activeSessionsByAttacker.put(attackerId, session);

        // Notify both players
        ModNetwork.sendQteSync(attacker, new QteSyncPacket(
            QteSyncPacket.Role.ATTACKER, sequence, duration, baseDamage));
        ModNetwork.sendQteSync(defender, new QteSyncPacket(
            QteSyncPacket.Role.DEFENDER, sequence, duration, baseDamage));

        ShinoRoi.LOGGER.debug("[ShinoRoi] QTE started: {} vs {} (technique: {})",
            attacker.getScoreboardName(), defender.getScoreboardName(), techniqueId);
    }

    /**
     * Handles a directional key press from a player during a QTE.
     *
     * @param playerUuid the UUID of the pressing player
     * @param direction  0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
     */
    public void handleKeyPress(UUID playerUuid, int direction, MinecraftServer server) {
        QteSession session = findSessionForPlayer(playerUuid);
        if (session == null || session.isResolved()) return;
        session.handlePress(playerUuid, direction);
    }

    /** Called every server tick (overworld). Resolves expired sessions. */
    public void tick(MinecraftServer server) {
        long currentTick = server.overworld().getGameTime();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, QteSession> entry : activeSessionsByAttacker.entrySet()) {
            QteSession session = entry.getValue();
            if (session.isResolved()) {
                toRemove.add(entry.getKey());
            } else if (session.isExpired(currentTick)) {
                resolveSession(session, server);
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(activeSessionsByAttacker::remove);
    }

    // ── Resolution ────────────────────────────────────────────────────────────

    private void resolveSession(QteSession session, MinecraftServer server) {
        session.markResolved();

        ServerPlayer attacker = server.getPlayerList().getPlayer(session.getAttackerUuid());
        ServerPlayer defender = server.getPlayerList().getPlayer(session.getDefenderUuid());

        if (attacker == null || defender == null) return;

        float multiplier = computeMultiplier(session.getBalance());
        float finalDamage = session.getBaseDamage() * multiplier;

        ShinoRoi.LOGGER.debug("[ShinoRoi] QTE resolved: balance={}, multiplier={}, damage={}",
            session.getBalance(), multiplier, finalDamage);

        if (finalDamage > 0f) {
            if (multiplier >= getOneshotThreshold()) {
                // One-shot: deal enough damage to kill the defender
                finalDamage = defender.getMaxHealth() * 1.5f;
            }
            defender.hurt(attacker.damageSources().playerAttack(attacker), finalDamage);
        }
    }

    /**
     * Computes the damage multiplier based on the final balance score.
     *
     * <pre>
     * balance ≤ nullifyThreshold → 0.0
     * balance = 0               → 1.0
     * balance ≥ oneshotThreshold → one-shot flag (≥ 5.0 sentinel)
     * linear interpolation between thresholds
     * </pre>
     */
    private float computeMultiplier(int balance) {
        int nullify = ModConfig.QTE_NULLIFY_THRESHOLD.get();   // negative
        int oneshot = ModConfig.QTE_ONESHOT_THRESHOLD.get();   // positive

        if (balance <= nullify) return 0f;
        if (balance >= oneshot) return getOneshotThreshold();

        if (balance < 0) {
            // defender gaining ground: interpolate from 0 at nullify to 1.0 at 0
            return 1f + (float) balance / Math.abs(nullify);
        } else {
            // attacker gaining ground: interpolate from 1.0 at 0 to oneshot at threshold
            return 1f + (getOneshotThreshold() - 1f) * balance / oneshot;
        }
    }

    private static float getOneshotThreshold() {
        return 5.0f; // sentinel for one-shot trigger
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QteSession findSessionForPlayer(UUID uuid) {
        // Check if they're an attacker
        QteSession s = activeSessionsByAttacker.get(uuid);
        if (s != null) return s;
        // Check if they're a defender
        for (QteSession session : activeSessionsByAttacker.values()) {
            if (session.getDefenderUuid().equals(uuid)) return session;
        }
        return null;
    }

    /** Generates an array of random directions (0–3). */
    private static int[] generateSequence(int length) {
        Random random = new Random();
        int[] seq = new int[length];
        for (int i = 0; i < length; i++) {
            seq[i] = random.nextInt(4);
        }
        return seq;
    }
}
