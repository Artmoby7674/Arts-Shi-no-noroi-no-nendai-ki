package com.shinoroi.data;

import net.minecraft.resources.ResourceLocation;

/**
 * Immutable definition of a single technique, loaded from JSON.
 *
 * <p>JSON path: {@code data/<namespace>/shinoroi_techniques/<id>.json}</p>
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "type": "NORMAL",
 *   "attackType": "MELEE",
 *   "moveset": "shinoroi:base",
 *   "animationTrigger": "shinoroi:slash_forward",
 *   "baseDamage": 8.0,
 *   "cooldownTicks": 40,
 *   "ultBarCost": 0.0,
 *   "skillPointCost": 2,
 *   "range": 4.0,
 *   "aoeRadius": 0.0,
 *   "terrainDestructRadius": 0,
 *   "displayName": "Forward Slash",
 *   "upgradeLevels": 3,
 *   "damagePerLevel": 2.0,
 *   "cooldownReductionPerLevel": 4
 * }
 * </pre></p>
 */
public record TechniqueDefinition(
    /** Registry ID (namespace:path), derived from the JSON file location. */
    ResourceLocation id,

    /** Whether this is a NORMAL, SECRET, or ULT technique. */
    TechniqueType type,

    /** Targeting / physics shape of this technique. */
    AttackType attackType,

    /** The moveset this technique belongs to. */
    ResourceLocation moveset,

    /**
     * Animation trigger ID fired via {@link com.shinoroi.event.CinematicTriggerEvent}.
     * Animation system hooks listen for this to play the correct clip.
     */
    ResourceLocation animationTrigger,

    /** Base damage at level 0. */
    float baseDamage,

    /** Cooldown in ticks (NORMAL techniques only; ignored for SECRET/ULT). */
    int cooldownTicks,

    /** Ult bar cost to activate (SECRET/ULT techniques; 0 for NORMAL). */
    float ultBarCost,

    /** Skill points required to unlock this technique. */
    int skillPointCost,

    /** Reach in blocks for MELEE; max range for RANGED. */
    float range,

    /** Blast radius in blocks for AOE; 0 for non-AOE. */
    float aoeRadius,

    /** Block destruction radius; 0 disables terrain destruction for this technique. */
    int terrainDestructRadius,

    /** Display name shown in the skill tree and hotbar tooltip. */
    String displayName,

    /** Maximum upgrade level (0 means no upgrades available). */
    int upgradeLevels,

    /** Extra damage per upgrade level. */
    float damagePerLevel,

    /** Cooldown tick reduction per upgrade level. */
    int cooldownReductionPerLevel
) {

    // ── Upgrade helpers ───────────────────────────────────────────────────────

    /**
     * Returns effective damage at the given upgrade level.
     *
     * @param level current upgrade level (0 = base)
     */
    public float effectiveDamage(int level) {
        return baseDamage + damagePerLevel * Math.max(0, level);
    }

    /**
     * Returns effective cooldown in ticks at the given upgrade level.
     *
     * @param level current upgrade level (0 = base)
     */
    public int effectiveCooldown(int level) {
        return Math.max(1, cooldownTicks - cooldownReductionPerLevel * Math.max(0, level));
    }
}
