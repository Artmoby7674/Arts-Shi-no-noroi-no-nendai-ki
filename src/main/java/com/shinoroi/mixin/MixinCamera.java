package com.shinoroi.mixin;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies a smooth right-shoulder camera offset during fight mode.
 *
 * Injected at the tail of Camera.setup() so the vanilla distance / angle
 * calculations are already done; we just add an extra lateral + vertical shift.
 *
 * Target offset when fight mode is fully active:
 *   +0.65 blocks to the right, +0.15 blocks up.
 *
 * The smooth field values are stored as @Unique instance fields on the Camera
 * singleton, so they persist across render frames and enable the lerp.
 */
@Mixin(Camera.class)
public class MixinCamera {

    @Unique private float shinoroi$smoothRight = 0f;
    @Unique private float shinoroi$smoothUp    = 0f;

    private static final float TARGET_RIGHT =  0.65f;
    private static final float TARGET_UP    =  0.15f;
    private static final float LERP_SPEED   =  0.12f;

    /** Shadowed so we can call Camera.move() from the inject. */
    @Shadow
    protected void move(double pDistanceOffset, double pVerticalOffset, double pHorizontalOffset) {}

    // remap = false: NeoForge 1.21+ uses Mojmap names at runtime; no SRG lookup needed.
    @Inject(method = "setup", at = @At("TAIL"), remap = false)
    private void shinoroi$shoulderCam(BlockGetter pLevel, Entity pEntity,
                                       boolean pThirdPerson, boolean pInverseView,
                                       float pPartialTick, CallbackInfo ci) {
        // Only apply in regular third-person (not the front-facing "inverse" view).
        if (!pThirdPerson || pInverseView) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || pEntity != player) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        boolean fightMode = data.isFightModeActive();

        float targetRight = fightMode ? TARGET_RIGHT : 0f;
        float targetUp    = fightMode ? TARGET_UP    : 0f;

        shinoroi$smoothRight += (targetRight - shinoroi$smoothRight) * LERP_SPEED;
        shinoroi$smoothUp    += (targetUp    - shinoroi$smoothUp)    * LERP_SPEED;

        // Snap to zero when effectively at rest to avoid permanent micro-drift.
        if (Math.abs(shinoroi$smoothRight) < 0.001f && Math.abs(shinoroi$smoothUp) < 0.001f) {
            shinoroi$smoothRight = 0f;
            shinoroi$smoothUp    = 0f;
            return;
        }

        // Camera.move(forward, vertical, horizontal):
        //   forward     > 0 → toward look target
        //   vertical    > 0 → upward (world Y)
        //   horizontal  > 0 → player's right (camera shifts right,
        //                      player appears left-of-centre on screen)
        move(0.0, shinoroi$smoothUp, shinoroi$smoothRight);
    }
}
