package com.shinoroi.mixin;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a right-shoulder camera offset when fight mode is active.
 *
 * After vanilla sets up the third-person-back camera position (directly
 * behind the player), we call move() a second time with a horizontal
 * offset to push the camera to the right shoulder.
 *
 * Offset values (tweak to taste):
 *   HORIZONTAL_OFFSET – positive = right side of the screen
 *   VERTICAL_OFFSET   – positive = slightly higher view
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    private static final double HORIZONTAL_OFFSET = 0.6;
    private static final double VERTICAL_OFFSET    = 0.1;

    /**
     * Camera.move(double distance, double verticalOffset, double horizontalOffset)
     * is protected, so we shadow it to call it from the mixin.
     */
    @Shadow
    protected abstract void move(double distance, double verticalOffset, double horizontalOffset);

    @Inject(method = "setup", at = @At("RETURN"))
    private void shinoroi$applyRightShoulderOffset(
            BlockGetter level, Entity entity,
            boolean detached, boolean mirrorFront,
            float partialTick, CallbackInfo ci) {

        // Only apply in third-person-back view (detached=true, mirrorFront=false)
        if (!detached || mirrorFront) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (!data.isFightModeActive()) return;

        // Shift the already-positioned camera to the right shoulder.
        // move(distanceForward, up, right) — negative distance keeps current depth.
        move(0.0, VERTICAL_OFFSET, HORIZONTAL_OFFSET);
    }
}
