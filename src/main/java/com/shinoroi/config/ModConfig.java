package com.shinoroi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * COMMON (server-authoritative) configuration for Shi no Noroi.
 *
 * <p>Values are readable on both sides after load. Client reads them for
 * display purposes; server uses them as the authoritative source.</p>
 *
 * Usage:
 * <pre>
 *   ModConfig.ULT_CHARGE_PER_DAMAGE.get()
 * </pre>
 */
public final class ModConfig {

    public static final ModConfigSpec SPEC;

    // ── Ult bar ──────────────────────────────────────────────────────────────

    /** Ult bar charge gained per 1 point of damage dealt. Default: 2.0 */
    public static final ModConfigSpec.DoubleValue ULT_CHARGE_PER_DAMAGE;

    // ── Fight-mode attribute bonuses ─────────────────────────────────────────

    /**
     * Armor bonus (ADD_VALUE on Attributes.ARMOR) applied while fight mode is active.
     * Does not affect max health — purely defensive. Default: 10.0 (5 armor icons).
     */
    public static final ModConfigSpec.DoubleValue DEFAULT_FIGHT_MODE_ARMOR_BONUS;

    /**
     * Minimum ticks between fight-mode toggles (anti-spam cooldown).
     * Default: 100 (5 seconds at 20 TPS).
     */
    public static final ModConfigSpec.IntValue TOGGLE_COOLDOWN_TICKS;

    /**
     * @deprecated No longer used — fight mode now grants armor instead of HP.
     * Kept so existing config files do not produce unknown-key warnings.
     */
    @Deprecated
    public static final ModConfigSpec.DoubleValue DEFAULT_FIGHT_MODE_HP_BONUS;

    // ── QTE balance system ───────────────────────────────────────────────────

    /** Duration of a QTE session in ticks. Default: 60 (3 seconds) */
    public static final ModConfigSpec.IntValue QTE_DURATION_TICKS;

    /** Number of arrow prompts in a QTE sequence. Default: 4 */
    public static final ModConfigSpec.IntValue QTE_SEQUENCE_LENGTH;

    /**
     * Balance value at which the attack is completely nullified (defender wins fully).
     * Must be negative. Default: -20
     */
    public static final ModConfigSpec.IntValue QTE_NULLIFY_THRESHOLD;

    /**
     * Balance value at which the attack becomes a one-shot (attacker wins fully).
     * Must be positive. Default: 20
     */
    public static final ModConfigSpec.IntValue QTE_ONESHOT_THRESHOLD;

    // ── Skill tree ───────────────────────────────────────────────────────────

    /** Skill points awarded per rank-up. Default: 3 */
    public static final ModConfigSpec.IntValue SKILL_POINTS_PER_RANK;

    // ── Terrain destruction ──────────────────────────────────────────────────

    /** Whether powerful techniques can destroy blocks. Default: true */
    public static final ModConfigSpec.BooleanValue TERRAIN_DESTRUCTION_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Shi no Noroi — Combat Configuration").push("combat");

        builder.comment("Ult bar").push("ultBar");
        ULT_CHARGE_PER_DAMAGE = builder
            .comment("Ult bar charge gained per 1 HP of damage dealt by the player.")
            .defineInRange("ultChargePerDamage", 2.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Fight mode").push("fightMode");
        DEFAULT_FIGHT_MODE_ARMOR_BONUS = builder
            .comment("Armor bonus applied while fight mode is active (ADD_VALUE on Attributes.ARMOR).",
                     "1 unit = 0.5 armor icons. Default 10 ≈ full iron armor.")
            .defineInRange("defaultFightModeArmorBonus", 10.0, 0.0, 100.0);
        TOGGLE_COOLDOWN_TICKS = builder
            .comment("Minimum ticks between fight-mode toggles (20 ticks = 1 second).")
            .defineInRange("toggleCooldownTicks", 100, 0, 1200);
        DEFAULT_FIGHT_MODE_HP_BONUS = builder
            .comment("(Deprecated — no longer applied. Retained for config file compatibility.)")
            .defineInRange("defaultFightModeHpBonus", 20.0, 0.0, 200.0);
        builder.pop();

        builder.comment("QTE balance system").push("qte");
        QTE_DURATION_TICKS = builder
            .comment("Duration of each QTE session in ticks (20 ticks = 1 second).")
            .defineInRange("qteDurationTicks", 60, 10, 200);
        QTE_SEQUENCE_LENGTH = builder
            .comment("Number of arrow prompts displayed per QTE session.")
            .defineInRange("qteSequenceLength", 4, 1, 10);
        QTE_NULLIFY_THRESHOLD = builder
            .comment("Balance score at which the attack is nullified (defender wins fully). Must be negative.")
            .defineInRange("qteNullifyThreshold", -20, -100, -1);
        QTE_ONESHOT_THRESHOLD = builder
            .comment("Balance score at which the attack is a one-shot (attacker wins fully). Must be positive.")
            .defineInRange("qteOneshotThreshold", 20, 1, 100);
        builder.pop();

        builder.comment("Skill tree").push("skillTree");
        SKILL_POINTS_PER_RANK = builder
            .comment("Skill points awarded per rank-up.")
            .defineInRange("skillPointsPerRank", 3, 1, 20);
        builder.pop();

        builder.comment("Terrain destruction").push("terrain");
        TERRAIN_DESTRUCTION_ENABLED = builder
            .comment("Whether powerful techniques can destroy blocks in their radius.")
            .define("terrainDestructionEnabled", true);
        builder.pop();

        builder.pop(); // combat

        SPEC = builder.build();
    }

    private ModConfig() {}
}
