package com.shinoroi.client.camera;

// ─────────────────────────────────────────────────────────────────────────────
// TODO: Replace these four imports with the actual package from your FreeCamera
//       API dependency.  The class/interface names must stay the same.
//       Add the dependency in build.gradle (see the TODO there).
//       Example: import com.example.freecam.api.CameraPlugin;
// ─────────────────────────────────────────────────────────────────────────────
import TODO_FREECAM_PKG.annotation.CameraPlugin;
import TODO_FREECAM_PKG.api.ICameraPlugin;
import TODO_FREECAM_PKG.api.ICameraModifier;
import TODO_FREECAM_PKG.api.ModifierPriority;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * FreeCamera plugin that positions the camera at a smooth right-shoulder offset
 * whenever the player is in fight mode.
 *
 * <p>When fight mode is active the camera lerps to:
 * <ul>
 *   <li>+0.65 blocks right of the player pivot</li>
 *   <li>+0.15 blocks up</li>
 *   <li>4.5 blocks behind (along look direction)</li>
 * </ul>
 *
 * <p>When fight mode exits, the offset smoothly returns to zero and the plugin
 * hands control back to the vanilla camera.
 *
 * <p>Registration is automatic via the {@code @CameraPlugin} annotation — no
 * explicit call in {@code ShinoRoi.java} is needed, as long as this class is
 * on the classpath when FreeCamera is present.
 */
@CameraPlugin(value = "shinoroi_fight_cam", priority = ModifierPriority.HIGH)
public class FightCameraPlugin implements ICameraPlugin {

    private ICameraModifier modifier;

    // Smoothed current values (lerped each frame)
    private float smoothX    = 0f;
    private float smoothY    = 0f;
    private float smoothDist = 4.5f;

    // Right-shoulder targets
    private static final float TARGET_X    =  0.65f;
    private static final float TARGET_Y    =  0.15f;
    private static final float TARGET_DIST =  4.5f;

    /** How fast the camera slides to/from the target position (per-frame factor). */
    private static final float LERP_SPEED = 0.12f;

    // ── ICameraPlugin lifecycle ───────────────────────────────────────────────

    @Override
    public void initialize(ICameraModifier modifier) {
        this.modifier = modifier;
        // Do not call modifier.enable() here — we enable dynamically per-frame.
    }

    /**
     * Called every render frame by the FreeCamera API.
     * Applies right-shoulder offset when fight mode is active; smoothly resets
     * when fight mode exits.
     *
     * @param partialTick fractional tick progress (0–1)
     */
    public void update(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        boolean fightMode = data.isFightModeActive();

        if (fightMode) {
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
            smoothX    = lerp(smoothX, 0f, LERP_SPEED);
            smoothY    = lerp(smoothY, 0f, LERP_SPEED);

            if (Math.abs(smoothX) > 0.005f || Math.abs(smoothY) > 0.005f) {
                modifier
                    .enablePos()
                    .setPos(smoothX, smoothY, 0f)
                    .move(0f, 0f, -smoothDist)
                    .enableObstacle();
            } else {
                // Fully reset — hand control back to vanilla camera
                smoothX = 0f;
                smoothY = 0f;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }
}
