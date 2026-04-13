package com.shinoroi.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shinoroi.ShinoRoi;
import com.shinoroi.data.AttackType;
import com.shinoroi.data.TechniqueDefinition;
import com.shinoroi.data.TechniqueType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads technique definitions from
 * {@code data/<namespace>/shinoroi_techniques/<id>.json}.
 *
 * <p>Register an instance of this class with
 * {@link net.neoforged.neoforge.event.AddReloadListenerEvent} on the game bus.</p>
 */
public class TechniqueRegistry extends SimpleJsonResourceReloadListener {

    public static final TechniqueRegistry INSTANCE = new TechniqueRegistry();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<ResourceLocation, TechniqueDefinition> techniques = new HashMap<>();

    private TechniqueRegistry() {
        super(GSON, "shinoroi_techniques");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        techniques.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                TechniqueDefinition def = parse(fileId, entry.getValue().getAsJsonObject());
                techniques.put(fileId, def);
            } catch (Exception e) {
                ShinoRoi.LOGGER.error("[ShinoRoi] Failed to parse technique '{}': {}",
                    fileId, e.getMessage());
            }
        }

        ShinoRoi.LOGGER.info("[ShinoRoi] Loaded {} technique definition(s).", techniques.size());
    }

    // ── Query API ─────────────────────────────────────────────────────────────

    /** Returns the definition for {@code id}, or {@code null} if not found. */
    public TechniqueDefinition get(ResourceLocation id) {
        return techniques.get(id);
    }

    /** Unmodifiable view of all loaded techniques. */
    public Collection<TechniqueDefinition> all() {
        return Collections.unmodifiableCollection(techniques.values());
    }

    /** All techniques belonging to the given moveset. */
    public List<TechniqueDefinition> forMoveset(ResourceLocation movesetId) {
        return techniques.values().stream()
            .filter(t -> movesetId.equals(t.moveset()))
            .collect(Collectors.toList());
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private static TechniqueDefinition parse(ResourceLocation id, JsonObject obj) {
        TechniqueType type = TechniqueType.valueOf(
            obj.get("type").getAsString().toUpperCase(Locale.ROOT));
        AttackType attackType = AttackType.valueOf(
            obj.get("attackType").getAsString().toUpperCase(Locale.ROOT));
        ResourceLocation moveset = ResourceLocation.parse(
            obj.get("moveset").getAsString());
        ResourceLocation animationTrigger = ResourceLocation.parse(
            obj.get("animationTrigger").getAsString());
        float baseDamage = obj.get("baseDamage").getAsFloat();
        int cooldownTicks = getInt(obj, "cooldownTicks", 0);
        float ultBarCost = getFloat(obj, "ultBarCost", 0f);
        int skillPointCost = getInt(obj, "skillPointCost", 1);
        float range = getFloat(obj, "range", 4f);
        float aoeRadius = getFloat(obj, "aoeRadius", 0f);
        int terrainDestructRadius = getInt(obj, "terrainDestructRadius", 0);
        String displayName = obj.has("displayName")
            ? obj.get("displayName").getAsString() : id.getPath();
        int upgradeLevels = getInt(obj, "upgradeLevels", 0);
        float damagePerLevel = getFloat(obj, "damagePerLevel", 0f);
        int cooldownReductionPerLevel = getInt(obj, "cooldownReductionPerLevel", 0);

        return new TechniqueDefinition(id, type, attackType, moveset, animationTrigger,
            baseDamage, cooldownTicks, ultBarCost, skillPointCost, range, aoeRadius,
            terrainDestructRadius, displayName, upgradeLevels, damagePerLevel,
            cooldownReductionPerLevel);
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        return obj.has(key) ? obj.get(key).getAsFloat() : def;
    }
}
