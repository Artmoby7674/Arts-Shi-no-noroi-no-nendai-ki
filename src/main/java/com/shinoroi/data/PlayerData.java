package com.shinoroi.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * All per-player state for the Shi no Noroi combat system.
 * Stored server-side via AttachmentTypes and synced to the client on change.
 */
public class PlayerData {

    private float energy;
    private float maxEnergy;
    private boolean fightModeActive;
    private int rank;
    /** technique id → game tick when the cooldown expires */
    private final Map<ResourceLocation, Long> cooldowns;
    private final List<ResourceLocation> unlockedTechniques;

    public PlayerData() {
        this(100f, 100f, false, 0, new HashMap<>(), new ArrayList<>());
    }

    public PlayerData(float energy, float maxEnergy, boolean fightModeActive, int rank,
                      Map<ResourceLocation, Long> cooldowns, List<ResourceLocation> unlockedTechniques) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.fightModeActive = fightModeActive;
        this.rank = rank;
        this.cooldowns = new HashMap<>(cooldowns);
        this.unlockedTechniques = new ArrayList<>(unlockedTechniques);
    }

    public static final MapCodec<PlayerData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("energy").orElse(100f).forGetter(PlayerData::getEnergy),
            Codec.FLOAT.fieldOf("maxEnergy").orElse(100f).forGetter(PlayerData::getMaxEnergy),
            Codec.BOOL.fieldOf("fightMode").orElse(false).forGetter(PlayerData::isFightModeActive),
            Codec.INT.fieldOf("rank").orElse(0).forGetter(PlayerData::getRank),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                .fieldOf("cooldowns").orElseGet(HashMap::new).forGetter(PlayerData::getCooldowns),
            ResourceLocation.CODEC.listOf()
                .fieldOf("techniques").orElseGet(ArrayList::new).forGetter(PlayerData::getUnlockedTechniques)
        ).apply(instance, PlayerData::new));
    public static final Codec<PlayerData> CODEC = MAP_CODEC.codec();

    // ── Getters ─────────────────────────────────────────────────────────────

    public float getEnergy() { return energy; }
    public float getMaxEnergy() { return maxEnergy; }
    public boolean isFightModeActive() { return fightModeActive; }
    public int getRank() { return rank; }
    public Map<ResourceLocation, Long> getCooldowns() { return Collections.unmodifiableMap(cooldowns); }
    public List<ResourceLocation> getUnlockedTechniques() { return Collections.unmodifiableList(unlockedTechniques); }

    // ── Setters / mutators ───────────────────────────────────────────────────

    public void setEnergy(float energy) {
        this.energy = Math.max(0f, Math.min(maxEnergy, energy));
    }

    public void setMaxEnergy(float maxEnergy) {
        this.maxEnergy = maxEnergy;
        this.energy = Math.min(this.energy, maxEnergy);
    }

    public void setFightModeActive(boolean fightModeActive) {
        this.fightModeActive = fightModeActive;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setCooldown(ResourceLocation technique, long expiresTick) {
        cooldowns.put(technique, expiresTick);
    }

    public boolean isOnCooldown(ResourceLocation technique, long currentTick) {
        return cooldowns.getOrDefault(technique, 0L) > currentTick;
    }

    public long getCooldownRemainingTicks(ResourceLocation technique, long currentTick) {
        return Math.max(0L, cooldowns.getOrDefault(technique, 0L) - currentTick);
    }

    public void unlockTechnique(ResourceLocation technique) {
        if (!unlockedTechniques.contains(technique)) {
            unlockedTechniques.add(technique);
        }
    }

    public boolean hasTechnique(ResourceLocation technique) {
        return unlockedTechniques.contains(technique);
    }

    /**
     * Attempts to spend energy. Returns true and deducts if sufficient, false otherwise.
     */
    public boolean consumeEnergy(float amount) {
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }
}
