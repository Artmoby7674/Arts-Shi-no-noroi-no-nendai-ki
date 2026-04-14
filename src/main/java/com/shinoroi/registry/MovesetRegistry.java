package com.shinoroi.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shinoroi.ShinoRoi;
import com.shinoroi.data.MovesetDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads moveset definitions from
 * {@code data/<namespace>/shinoroi_movesets/<id>.json}.
 *
 * <p>Register an instance of this class with
 * {@link net.neoforged.neoforge.event.AddReloadListenerEvent} on the game bus.</p>
 */
public class MovesetRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final MovesetRegistry INSTANCE = new MovesetRegistry();

    private final Map<ResourceLocation, MovesetDefinition> movesets = new HashMap<>();

    private MovesetRegistry() {
        super(GSON, "shinoroi_movesets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        movesets.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                MovesetDefinition def = parse(fileId, entry.getValue().getAsJsonObject());
                movesets.put(fileId, def);
            } catch (Exception e) {
                ShinoRoi.LOGGER.error("[ShinoRoi] Failed to parse moveset '{}': {}",
                    fileId, e.getMessage());
            }
        }

        ShinoRoi.LOGGER.info("[ShinoRoi] Loaded {} moveset definition(s).", movesets.size());
    }

    // ── Query API ─────────────────────────────────────────────────────────────

    /** Returns the definition for {@code id}, or {@code null} if not found. */
    public MovesetDefinition get(ResourceLocation id) {
        return movesets.get(id);
    }

    /** Looks up by string ID; returns {@code null} if blank or not found. */
    public MovesetDefinition getByString(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return get(ResourceLocation.parse(id));
        } catch (Exception e) {
            return null;
        }
    }

    /** Unmodifiable view of all loaded movesets. */
    public Collection<MovesetDefinition> all() {
        return Collections.unmodifiableCollection(movesets.values());
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private static MovesetDefinition parse(ResourceLocation id, JsonObject obj) {
        String displayName = obj.has("displayName")
            ? obj.get("displayName").getAsString() : id.getPath();
        double fightModeHpBonus = obj.has("fightModeHpBonus")
            ? obj.get("fightModeHpBonus").getAsDouble() : -1.0;
        double ultBarChargeRate = obj.has("ultBarChargeRate")
            ? obj.get("ultBarChargeRate").getAsDouble() : 1.0;

        List<ResourceLocation> techniques = new ArrayList<>();
        if (obj.has("techniques")) {
            for (JsonElement el : obj.getAsJsonArray("techniques")) {
                try {
                    techniques.add(ResourceLocation.parse(el.getAsString()));
                } catch (Exception e) {
                    ShinoRoi.LOGGER.warn("[ShinoRoi] Moveset '{}': invalid technique id '{}'",
                        id, el.getAsString());
                }
            }
        }

        return new MovesetDefinition(id, displayName, fightModeHpBonus, ultBarChargeRate, techniques);
    }
}
