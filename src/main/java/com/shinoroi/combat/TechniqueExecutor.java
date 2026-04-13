package com.shinoroi.combat;

import com.shinoroi.ShinoRoi;
import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.MovesetDefinition;
import com.shinoroi.data.PlayerData;
import com.shinoroi.data.TechniqueDefinition;
import com.shinoroi.data.TechniqueType;
import com.shinoroi.event.CinematicTriggerEvent;
import com.shinoroi.network.ModNetwork;
import com.shinoroi.registry.MovesetRegistry;
import com.shinoroi.registry.TechniqueRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Server-side technique activation logic.
 *
 * <p>Called from {@link com.shinoroi.network.TechniqueActivatePacket} handler.</p>
 *
 * <p>Execution flow:</p>
 * <ol>
 *   <li>Validate fight mode, unlocked, not on cooldown, ult bar sufficient</li>
 *   <li>Find target(s) based on {@code attackType}</li>
 *   <li>For mob targets: apply damage directly</li>
 *   <li>For player targets: start {@link QteSession}</li>
 *   <li>Apply terrain destruction if configured</li>
 *   <li>Fire {@link CinematicTriggerEvent}</li>
 *   <li>Set cooldown / deduct ult bar / sync to client</li>
 * </ol>
 */
public final class TechniqueExecutor {

    private TechniqueExecutor() {}

    /**
     * Attempts to execute the technique in the given hotbar slot for the player.
     *
     * @param player the acting server player
     * @param slot   hotbar slot index (0-based)
     */
    public static void execute(ServerPlayer player, int slot) {
        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());

        if (!data.isFightModeActive()) return;

        // Resolve moveset → technique list
        MovesetDefinition moveset = MovesetRegistry.INSTANCE.getByString(data.getActiveMovesetId());
        if (moveset == null || slot >= moveset.techniques().size()) return;

        ResourceLocation techniqueId = moveset.techniques().get(slot);
        TechniqueDefinition def = TechniqueRegistry.INSTANCE.get(techniqueId);
        if (def == null) {
            ShinoRoi.LOGGER.warn("[ShinoRoi] Technique not loaded: {}", techniqueId);
            return;
        }

        // Validate unlock
        if (!data.hasTechnique(techniqueId)) return;

        // Validate cooldown
        long gameTick = player.serverLevel().getGameTime();
        if (data.isOnCooldown(techniqueId, gameTick)) return;

        // Validate ult bar for SECRET / ULT
        if (def.type() == TechniqueType.SECRET || def.type() == TechniqueType.ULT) {
            if (!data.consumeUlt(def.ultBarCost())) return;
        }

        // Determine upgrade level
        int upgradeLevel = data.getUpgradeLevel(techniqueId);
        float damage = def.effectiveDamage(upgradeLevel);

        // ── Target acquisition ────────────────────────────────────────────────
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        LivingEntity primaryTarget = null;

        switch (def.attackType()) {
            case MELEE -> {
                primaryTarget = findMeleeTarget(player, level, origin, lookVec, def.range());
                if (primaryTarget != null) {
                    applyToTarget(player, primaryTarget, techniqueId, damage, data,
                        player.server, level, def);
                }
            }
            case RANGED -> {
                primaryTarget = findRangedTarget(player, level, origin, lookVec, def.range());
                if (primaryTarget != null) {
                    applyToTarget(player, primaryTarget, techniqueId, damage, data,
                        player.server, level, def);
                }
            }
            case AOE -> {
                List<LivingEntity> targets = findAoeTargets(player, level,
                    origin.add(lookVec.scale(def.range() * 0.5)), def.aoeRadius());
                for (LivingEntity t : targets) {
                    applyToTarget(player, t, techniqueId, damage, data,
                        player.server, level, def);
                    if (primaryTarget == null) primaryTarget = t;
                }
            }
        }

        // ── Terrain destruction ───────────────────────────────────────────────
        if (def.terrainDestructRadius() > 0) {
            Vec3 impactPoint = primaryTarget != null
                ? primaryTarget.position()
                : origin.add(lookVec.scale(def.range()));
            TerrainDestructor.destroyRadius(level, impactPoint, def.terrainDestructRadius(), player);
        }

        // ── Cinematic trigger ─────────────────────────────────────────────────
        NeoForge.EVENT_BUS.post(new CinematicTriggerEvent(player, def.animationTrigger(), primaryTarget));

        // ── Cooldown (NORMAL only; SECRET/ULT ult bar was already deducted) ──
        if (def.type() == TechniqueType.NORMAL) {
            data.setCooldown(techniqueId, gameTick + def.effectiveCooldown(upgradeLevel));
        }

        // Persist and sync
        player.setData(ModAttachments.PLAYER_DATA.get(), data);
        ModNetwork.syncToClient(player, data);
    }

    // ── Target helpers ────────────────────────────────────────────────────────

    private static void applyToTarget(ServerPlayer attacker, LivingEntity target,
                                       ResourceLocation techniqueId, float damage,
                                       PlayerData data, MinecraftServer server,
                                       ServerLevel level, TechniqueDefinition def) {
        if (target instanceof ServerPlayer defenderPlayer) {
            // Player vs player → QTE
            QteManager.INSTANCE.startQte(attacker, defenderPlayer, techniqueId, damage, server);
        } else {
            // Mob or other living entity → direct damage
            target.hurt(attacker.damageSources().playerAttack(attacker), damage);
        }
    }

    private static LivingEntity findMeleeTarget(ServerPlayer player, ServerLevel level,
                                                  Vec3 origin, Vec3 look, float range) {
        Vec3 end = origin.add(look.scale(range));
        AABB box = new AABB(origin, end).inflate(1.0);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive())
            .stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .orElse(null);
    }

    private static LivingEntity findRangedTarget(ServerPlayer player, ServerLevel level,
                                                   Vec3 origin, Vec3 look, float range) {
        // Use a narrower search along the look vector
        Vec3 end = origin.add(look.scale(range));
        AABB box = new AABB(origin, end).inflate(0.5);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive())
            .stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .orElse(null);
    }

    private static List<LivingEntity> findAoeTargets(ServerPlayer player, ServerLevel level,
                                                       Vec3 center, float radius) {
        AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        return level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e != player && e.isAlive()
                && e.distanceToSqr(center.x, center.y, center.z) <= radius * radius);
    }
}
