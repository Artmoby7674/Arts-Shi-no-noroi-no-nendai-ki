package com.shinoroi.client.camera;

import cn.anecansaitin.freecameraapi.api.CameraPlugin;
import cn.anecansaitin.freecameraapi.api.ICameraModifier;
import cn.anecansaitin.freecameraapi.api.ICameraPlugin;
import cn.anecansaitin.freecameraapi.api.ModifierPriority;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * FreeCamera API plugin that positions the camera at a smooth right-shoulder
 * offset whenever the player is in fight mode.
 *
 * When fight mode is active the camera lerps to:
 *  +0.65 blocks right, +0.15 blocks up, 4.5 blocks back (along look direction).
 *
 * When fight mode exits the offset smoothly returns to zero, then the modifier
 * is disabled so the vanilla camera resumes full control.
 *
 * Registration is automatic via the @CameraPlugin annotation — no explicit call
 * in ShinoRoi.java is needed as long as this class is on the classpath when
 * Free Camera API is present.
 *
 * Dependency: place free_camera_api-neo-1.21.1-3.2.0.jar in the libs/ folder.
 */
@CameraPlugin(value = "shinoroi_fight_cam", priority = ModifierPriority.HIGH)
public class FightCameraPlugin implements ICameraPlugin {

    private ICameraModifier modifier;

    // Smoothed current values (lerped each render frame)
    private float smoothX    = 0f;
    private float smoothY    = 0f;
    private float smoothDist = 4.5f;

    // Right-shoulder targets
    private static final float TARGET_X    =  0.65f;
    private static final float TARGET_Y    =  0.15f;
    private static final float TARGET_DIST =  4.5f;

    /** Per-frame lerp speed (frame-rate independent smoothness approximation). */
    private static final float LERP_SPEED = 0.12f;

    // ── ICameraPlugin lifecycle ───────────────────────────────────────────────

    @Override
    public void initialize(ICameraModifier modifier) {
        this.modifier = modifier;
        // Start enabled — update() checks fight mode and calls disable() if inactive.
        modifier.enable();
    }

    /**
     * Called every render frame by the Free Camera API (no parameters — use
     * Minecraft state for timing if needed).
     */
    @Override
    public void update() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        boolean fightMode = data.isFightModeActive();

        if (fightMode) {
            modifier.enable();

            // Lerp toward right-shoulder position
            smoothX    = lerp(smoothX,    TARGET_X,    LERP_SPEED);
            smoothY    = lerp(smoothY,    TARGET_Y,    LERP_SPEED);
            smoothDist = lerp(smoothDist, TARGET_DIST, LERP_SPEED);

            modifier
                .enablePos()
                .setPos(smoothX, smoothY, 0f)
                .move(0f, 0f, -smoothDist)
                .enableObstacle();

        } else {
            // Lerp back to centre; keep applying until effectively zero
            smoothX = lerp(smoothX, 0f, LERP_SPEED);
            smoothY = lerp(smoothY, 0f, LERP_SPEED);

            if (Math.abs(smoothX) > 0.005f || Math.abs(smoothY) > 0.005f) {
                modifier.enable();
                modifier
                    .enablePos()
                    .setPos(smoothX, smoothY, 0f)
                    .move(0f, 0f, -smoothDist)
                    .enableObstacle();
            } else {
                smoothX = 0f;
                smoothY = 0f;
                // Fully reset — hand control back to vanilla camera
                modifier.disable();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }
}
