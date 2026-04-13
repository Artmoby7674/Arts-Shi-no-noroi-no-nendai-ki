package com.shinoroi.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Immutable definition of a moveset, loaded from JSON.
 *
 * <p>A moveset groups a set of techniques for a specific combat style or
 * character archetype. Players equip one moveset at a time.</p>
 *
 * <p>JSON path: {@code data/<namespace>/shinoroi_movesets/<id>.json}</p>
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "displayName": "Cursed Blade Style",
 *   "fightModeHpBonus": 30.0,
 *   "ultBarChargeRate": 1.0,
 *   "techniques": [
 *     "shinoroi:slash_forward",
 *     "shinoroi:rising_slash",
 *     "shinoroi:cursed_wave",
 *     "shinoroi:domain_expansion"
 *   ]
 * }
 * </pre></p>
 */
public record MovesetDefinition(
    /** Registry ID (namespace:path), derived from the JSON file location. */
    ResourceLocation id,

    /** Display name shown in the skill tree screen header. */
    String displayName,

    /**
     * HP bonus (AttributeModifier ADD_VALUE) applied while fight mode is active.
     * Overrides {@link com.shinoroi.config.ModConfig#DEFAULT_FIGHT_MODE_HP_BONUS}.
     * Set to -1 to use the config default.
     */
    double fightModeHpBonus,

    /**
     * Multiplier applied on top of the base ult charge per damage.
     * 1.0 = no change; 2.0 = double charge rate.
     */
    double ultBarChargeRate,

    /**
     * Ordered list of technique IDs available in this moveset's hotbar slots.
     * Slots 0–8 map to hotbar positions; extra entries are ignored.
     */
    List<ResourceLocation> techniques
) {}
