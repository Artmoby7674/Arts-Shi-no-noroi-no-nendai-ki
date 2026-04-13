package com.shinoroi.combat;

import com.shinoroi.ShinoRoi;
import com.shinoroi.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Destroys blocks in a sphere radius around an impact point.
 * Players are never harmed by terrain destruction.
 *
 * <p>Only operates on the server. Terrain destruction can be globally
 * disabled via {@link ModConfig#TERRAIN_DESTRUCTION_ENABLED}.</p>
 */
public final class TerrainDestructor {

    private TerrainDestructor() {}

    /**
     * Destroys blocks within {@code radius} blocks of {@code center}.
     * Air, bedrock, and barrier blocks are never destroyed.
     *
     * @param level   server level
     * @param center  impact point
     * @param radius  destruction radius in blocks
     * @param caster  player responsible (for logging/attribution)
     */
    public static void destroyRadius(ServerLevel level, Vec3 center, int radius, Player caster) {
        if (!ModConfig.TERRAIN_DESTRUCTION_ENABLED.get()) return;
        if (radius <= 0) return;

        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        double radiusSq = (double) radius * radius;

        int destroyed = 0;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distSq = (x - center.x) * (x - center.x)
                        + (y - center.y) * (y - center.y)
                        + (z - center.z) * (z - center.z);
                    if (distSq > radiusSq) continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);

                    if (state.isAir()) continue;
                    if (state.is(Blocks.BEDROCK)) continue;
                    if (state.is(Blocks.BARRIER)) continue;

                    level.removeBlock(pos, false);
                    destroyed++;
                }
            }
        }

        ShinoRoi.LOGGER.debug("[ShinoRoi] Terrain destruction by {}: {} blocks in radius {}",
            caster.getScoreboardName(), destroyed, radius);
    }
}
