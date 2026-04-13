package com.shinoroi.combat;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Represents a single active QTE (Quick-Time Event) balance duel between two players.
 *
 * <p>The QTE balance mechanic:</p>
 * <ul>
 *   <li>Both attacker and defender see the same random arrow sequence.</li>
 *   <li>Attacker presses correct arrow → balance + 1 (attacker winning).</li>
 *   <li>Defender presses correct arrow → balance - 1 (defender winning).</li>
 *   <li>At session end, balance determines a damage multiplier:
 *       <ul>
 *         <li>balance ≤ nullifyThreshold → attack nullified (0× damage)</li>
 *         <li>balance ≥ oneshotThreshold → one-shot (full HP damage)</li>
 *         <li>linear interpolation in between, base at 0</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class QteSession {

    private final UUID attackerUuid;
    private final UUID defenderUuid;
    private final ResourceLocation techniqueId;

    /**
     * Arrow sequence: values 0–3 correspond to UP, DOWN, LEFT, RIGHT.
     * Generated once at session start; both players see the same sequence.
     */
    private final int[] sequence;

    /** Current balance score. Starts at 0. Positive = attacker winning. */
    private int balance;

    /** Game tick when this session started. */
    private final long startTick;

    /** Duration in ticks (from config at creation time). */
    private final int durationTicks;

    /** Base damage before QTE multiplier is applied. */
    private final float baseDamage;

    /** Which sequence index the attacker is on (0-based). */
    private int attackerIndex;

    /** Which sequence index the defender is on (0-based). */
    private int defenderIndex;

    private boolean resolved;

    public QteSession(UUID attackerUuid, UUID defenderUuid, ResourceLocation techniqueId,
                      int[] sequence, long startTick, int durationTicks, float baseDamage) {
        this.attackerUuid = attackerUuid;
        this.defenderUuid = defenderUuid;
        this.techniqueId = techniqueId;
        this.sequence = sequence.clone();
        this.balance = 0;
        this.startTick = startTick;
        this.durationTicks = durationTicks;
        this.baseDamage = baseDamage;
        this.attackerIndex = 0;
        this.defenderIndex = 0;
        this.resolved = false;
    }

    // ── Input handling ────────────────────────────────────────────────────────

    /**
     * Records a key press from a participant.
     *
     * @param uuid      the player who pressed
     * @param direction the direction pressed (0=UP, 1=DOWN, 2=LEFT, 3=RIGHT)
     */
    public void handlePress(UUID uuid, int direction) {
        if (resolved) return;

        if (uuid.equals(attackerUuid) && attackerIndex < sequence.length) {
            if (direction == sequence[attackerIndex]) {
                balance++;
            }
            attackerIndex++;
        } else if (uuid.equals(defenderUuid) && defenderIndex < sequence.length) {
            if (direction == sequence[defenderIndex]) {
                balance--;
            }
            defenderIndex++;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public boolean isExpired(long currentTick) {
        return currentTick >= startTick + durationTicks;
    }

    public boolean isResolved() { return resolved; }
    public void markResolved() { this.resolved = true; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getAttackerUuid() { return attackerUuid; }
    public UUID getDefenderUuid() { return defenderUuid; }
    public ResourceLocation getTechniqueId() { return techniqueId; }
    public int[] getSequence() { return sequence.clone(); }
    public int getBalance() { return balance; }
    public long getStartTick() { return startTick; }
    public int getDurationTicks() { return durationTicks; }
    public float getBaseDamage() { return baseDamage; }
}
