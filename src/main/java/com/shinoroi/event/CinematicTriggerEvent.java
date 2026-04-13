package com.shinoroi.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the NeoForge game event bus when a technique activation should
 * trigger a cinematic or animation sequence.
 *
 * <p>External animation systems (e.g. GeckoLib, custom render hooks) can
 * subscribe to this event to play the animation identified by
 * {@link #animationId()}.</p>
 *
 * <p>This event is not cancellable; it is purely informational.</p>
 */
public class CinematicTriggerEvent extends Event {

    private final Player caster;
    private final ResourceLocation animationId;
    @Nullable
    private final LivingEntity target;

    /**
     * @param caster      the player who activated the technique
     * @param animationId ResourceLocation matching the animation trigger defined
     *                    in the technique JSON (e.g. {@code shinoroi:domain_expansion})
     * @param target      the primary target entity, or {@code null} for AOE / self techniques
     */
    public CinematicTriggerEvent(Player caster, ResourceLocation animationId,
                                  @Nullable LivingEntity target) {
        this.caster = caster;
        this.animationId = animationId;
        this.target = target;
    }

    /** The player who triggered the animation. */
    public Player getCaster() {
        return caster;
    }

    /**
     * The animation trigger ID, matching the {@code animationTrigger} field
     * in the technique's JSON definition.
     */
    public ResourceLocation getAnimationId() {
        return animationId;
    }

    /**
     * The primary target, or {@code null} for area/self techniques.
     */
    @Nullable
    public LivingEntity getTarget() {
        return target;
    }
}
