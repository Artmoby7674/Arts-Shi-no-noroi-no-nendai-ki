package com.shinoroi.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * All per-player state for the Shi no Noroi combat system.
 * Stored server-side via AttachmentTypes and synced to the client on change.
 *
 * Fields:
 *  - ultBar             : current ult bar charge (0–100)
 *  - fightModeActive    : whether fight mode is toggled on
 *  - rank               : player's current rank (used for skill tree gating)
 *  - selectedSlot       : which hotbar slot is currently selected (0-based)
 *  - skillPoints        : unspent skill points available for unlocks/upgrades
 *  - activeMovesetId    : ID string of the equipped moveset ("" = none)
 *  - cooldowns          : technique id → game tick when cooldown expires
 *  - unlockedTechniques : list of unlocked technique IDs
 *  - upgradeLevels      : technique id → upgrade level (0 = base)
 *  - storedInventory    : items saved when fight mode is entered (restored on exit)
 *  - lastToggleTick     : game tick of the last fight-mode toggle (cooldown guard)
 */
public class PlayerData {

    private float ultBar;
    private boolean fightModeActive;
    private int rank;
    private int selectedSlot;
    private int skillPoints;
    private String activeMovesetId;
    private final Map<ResourceLocation, Long> cooldowns;
    private final List<ResourceLocation> unlockedTechniques;
    private final Map<ResourceLocation, Integer> upgradeLevels;
    /** Items (main 36 + offhand 1) stashed when fight mode is entered. */
    private final List<ItemStack> storedInventory;
    /** Server game-tick when fight mode was last toggled (used for cooldown). */
    private long lastToggleTick;

    public PlayerData() {
        this(0f, false, 0, 0, 0, "",
             new HashMap<>(), new ArrayList<>(), new HashMap<>(),
             new ArrayList<>(), 0L);
    }

    public PlayerData(float ultBar, boolean fightModeActive, int rank,
                      int selectedSlot, int skillPoints, String activeMovesetId,
                      Map<ResourceLocation, Long> cooldowns,
                      List<ResourceLocation> unlockedTechniques,
                      Map<ResourceLocation, Integer> upgradeLevels,
                      List<ItemStack> storedInventory,
                      long lastToggleTick) {
        this.ultBar = Math.max(0f, Math.min(100f, ultBar));
        this.fightModeActive = fightModeActive;
        this.rank = rank;
        this.selectedSlot = selectedSlot;
        this.skillPoints = skillPoints;
        this.activeMovesetId = activeMovesetId;
        this.cooldowns = new HashMap<>(cooldowns);
        this.unlockedTechniques = new ArrayList<>(unlockedTechniques);
        this.upgradeLevels = new HashMap<>(upgradeLevels);
        this.storedInventory = new ArrayList<>(storedInventory);
        this.lastToggleTick = lastToggleTick;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static final MapCodec<PlayerData> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("ultBar").orElse(0f).forGetter(PlayerData::getUltBar),
            Codec.BOOL.fieldOf("fightMode").orElse(false).forGetter(PlayerData::isFightModeActive),
            Codec.INT.fieldOf("rank").orElse(0).forGetter(PlayerData::getRank),
            Codec.INT.fieldOf("selectedSlot").orElse(0).forGetter(PlayerData::getSelectedSlot),
            Codec.INT.fieldOf("skillPoints").orElse(0).forGetter(PlayerData::getSkillPoints),
            Codec.STRING.fieldOf("activeMovesetId").orElse("").forGetter(PlayerData::getActiveMovesetId),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                .fieldOf("cooldowns").orElseGet(HashMap::new).forGetter(PlayerData::getCooldowns),
            ResourceLocation.CODEC.listOf()
                .fieldOf("techniques").orElseGet(ArrayList::new).forGetter(PlayerData::getUnlockedTechniques),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                .fieldOf("upgradeLevels").orElseGet(HashMap::new).forGetter(PlayerData::getUpgradeLevels),
            ItemStack.OPTIONAL_CODEC.listOf()
                .fieldOf("storedInventory").orElseGet(ArrayList::new).forGetter(PlayerData::getStoredInventory),
            Codec.LONG.fieldOf("lastToggleTick").orElse(0L).forGetter(PlayerData::getLastToggleTick)
        ).apply(instance, PlayerData::new));

    public static final Codec<PlayerData> CODEC = MAP_CODEC.codec();

    // ── Getters ──────────────────────────────────────────────────────────────

    public float getUltBar() { return ultBar; }
    public boolean isFightModeActive() { return fightModeActive; }
    public int getRank() { return rank; }
    public int getSelectedSlot() { return selectedSlot; }
    public int getSkillPoints() { return skillPoints; }
    public String getActiveMovesetId() { return activeMovesetId; }
    public Map<ResourceLocation, Long> getCooldowns() { return Collections.unmodifiableMap(cooldowns); }
    public List<ResourceLocation> getUnlockedTechniques() { return Collections.unmodifiableList(unlockedTechniques); }
    public Map<ResourceLocation, Integer> getUpgradeLevels() { return Collections.unmodifiableMap(upgradeLevels); }
    public List<ItemStack> getStoredInventory() { return Collections.unmodifiableList(storedInventory); }
    public long getLastToggleTick() { return lastToggleTick; }

    // ── Ult bar ───────────────────────────────────────────────────────────────

    public void addUltCharge(float amount) {
        this.ultBar = Math.max(0f, Math.min(100f, this.ultBar + amount));
    }

    public boolean consumeUlt(float amount) {
        if (ultBar < amount) return false;
        ultBar = Math.max(0f, ultBar - amount);
        return true;
    }

    public void setUltBar(float ultBar) {
        this.ultBar = Math.max(0f, Math.min(100f, ultBar));
    }

    // ── Fight mode ────────────────────────────────────────────────────────────

    public void setFightModeActive(boolean fightModeActive) {
        this.fightModeActive = fightModeActive;
    }

    // ── Toggle cooldown ───────────────────────────────────────────────────────

    public void setLastToggleTick(long tick) {
        this.lastToggleTick = tick;
    }

    // ── Stored inventory ─────────────────────────────────────────────────────

    /**
     * Saves a copy of the given item list as the stored inventory.
     * Call when entering fight mode to preserve the player's items.
     */
    public void storeInventory(List<ItemStack> items) {
        storedInventory.clear();
        for (ItemStack item : items) {
            storedInventory.add(item.copy());
        }
    }

    /** Clears the stored inventory. Call after restoring items to the player. */
    public void clearStoredInventory() {
        storedInventory.clear();
    }

    // ── Rank / skill points ───────────────────────────────────────────────────

    public void setRank(int rank) { this.rank = rank; }

    public void addSkillPoints(int amount) {
        this.skillPoints = Math.max(0, this.skillPoints + amount);
    }

    public boolean spendSkillPoints(int amount) {
        if (skillPoints < amount) return false;
        skillPoints -= amount;
        return true;
    }

    // ── Hotbar slot ───────────────────────────────────────────────────────────

    public void setSelectedSlot(int slot) {
        this.selectedSlot = Math.max(0, slot);
    }

    // ── Moveset ───────────────────────────────────────────────────────────────

    public void setActiveMovesetId(String id) {
        this.activeMovesetId = id == null ? "" : id;
    }

    // ── Cooldowns ─────────────────────────────────────────────────────────────

    public void setCooldown(ResourceLocation technique, long expiresTick) {
        cooldowns.put(technique, expiresTick);
    }

    public boolean isOnCooldown(ResourceLocation technique, long currentTick) {
        return cooldowns.getOrDefault(technique, 0L) > currentTick;
    }

    public long getCooldownRemainingTicks(ResourceLocation technique, long currentTick) {
        return Math.max(0L, cooldowns.getOrDefault(technique, 0L) - currentTick);
    }

    // ── Technique unlock / upgrade ────────────────────────────────────────────

    public void unlockTechnique(ResourceLocation technique) {
        if (!unlockedTechniques.contains(technique)) {
            unlockedTechniques.add(technique);
            upgradeLevels.putIfAbsent(technique, 0);
        }
    }

    public boolean hasTechnique(ResourceLocation technique) {
        return unlockedTechniques.contains(technique);
    }

    public int getUpgradeLevel(ResourceLocation technique) {
        return upgradeLevels.getOrDefault(technique, 0);
    }

    public boolean upgradeTechnique(ResourceLocation technique, int maxLevel, int skillPointCost) {
        if (!hasTechnique(technique)) return false;
        int current = getUpgradeLevel(technique);
        if (current >= maxLevel) return false;
        if (!spendSkillPoints(skillPointCost)) return false;
        upgradeLevels.put(technique, current + 1);
        return true;
    }
}
