package com.shinoroi.client.camera;

import cn.anecansaitin.freecameraapi.api.CameraPlugin;
import cn.anecansaitin.freecameraapi.api.ICameraModifier;
import cn.anecansaitin.freecameraapi.api.ICameraPlugin;
import cn.anecansaitin.freecameraapi.api.ModifierPriority;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * FreeCamera API plugin that positions the camera at a smooth right-shoulder
 * offset whenever the player is in fight mode.
 *
 * When fight mode is active the camera lerps to:
 *  +0.6 blocks right (relative to horizontal look direction)
 *  +0.15 blocks above the player's eye
 *  2.5 blocks back (horizontal only — never dips underground on steep pitch)
 *
 * The back-distance uses only the horizontal (yaw-only) component of the look
 * direction so the camera stays stable when the player looks up or down.
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

    // Smoothed values (lerped each frame). Both start at 0 so the camera
    // animates in smoothly on the first fight-mode entry.
    private float smoothX    = 0f;  // right offset (blocks)
    private float smoothDist = 0f;  // backward distance (blocks)

    // Right-shoulder targets
    private static final float TARGET_X    =  0.6f;   // blocks right of horizontal look
    private static final float TARGET_Y    =  0.15f;  // blocks above eye (constant, not smoothed)
    private static final float TARGET_DIST =  2.5f;   // blocks back (horizontal only)

    /** Per-frame lerp speed (frame-rate independent approximation). */
    private static final float LERP_SPEED = 0.12f;

    // ── ICameraPlugin lifecycle ───────────────────────────────────────────────

    @Override
    public void initialize(ICameraModifier modifier) {
        this.modifier = modifier;
        modifier.enable();
    }

    /**
     * Called every render frame by the Free Camera API.
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
            smoothX    = lerp(smoothX,    TARGET_X,    LERP_SPEED);
            smoothDist = lerp(smoothDist, TARGET_DIST, LERP_SPEED);
            applyShoulderCamera(player);

        } else {
            // Lerp back toward centre
            smoothX    = lerp(smoothX,    0f, LERP_SPEED);
            smoothDist = lerp(smoothDist, 0f, LERP_SPEED);

            if (Math.abs(smoothX) > 0.005f || Math.abs(smoothDist) > 0.01f) {
                modifier.enable();
                applyShoulderCamera(player);
            } else {
                smoothX    = 0f;
                smoothDist = 0f;
                modifier.disable();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Positions the camera at the right-shoulder offset relative to the
     * player's eye in world-aligned coordinates.
     *
     * The right and back components are derived from the player's horizontal
     * (yaw-only) look direction so the camera never dips underground when the
     * player pitches up or down.
     *
     *   offset.x = rightX * smoothX  -  fwdX * smoothDist
     *   offset.y = TARGET_Y
     *   offset.z = rightZ * smoothX  -  fwdZ * smoothDist
     *
     * where (rightX, rightZ) is the unit right vector 90° CW from (fwdX, fwdZ).
     */
    private void applyShoulderCamera(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        double hLen = Math.sqrt(look.x * look.x + look.z * look.z);

        // Horizontal forward unit vector (safe fallback when looking straight up/down)
        double fwdX = hLen > 1e-6 ? look.x / hLen : 0.0;
        double fwdZ = hLen > 1e-6 ? look.z / hLen : 1.0;

        // Horizontal right = 90° clockwise rotation of forward: (fwdZ, 0, -fwdX)
        double rX = fwdZ;
        double rZ = -fwdX;

        float ox = (float)(rX * smoothX - fwdX * smoothDist);
        float oy = TARGET_Y;
        float oz = (float)(rZ * smoothX - fwdZ * smoothDist);

        modifier.enablePos()
                .setPos(ox, oy, oz)
                .enableObstacle();
    }

    private static float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }
}
