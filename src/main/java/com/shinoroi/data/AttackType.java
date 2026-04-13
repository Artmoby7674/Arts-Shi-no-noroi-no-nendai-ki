package com.shinoroi.data;

/**
 * Physical form a technique takes when it activates.
 * Used by TechniqueExecutor to determine targeting strategy.
 */
public enum AttackType {
    /** Single target in front of the player (raycast). */
    MELEE,
    /** Projectile aimed at a target (ranged raycast). */
    RANGED,
    /** All entities within a radius sphere. */
    AOE
}
