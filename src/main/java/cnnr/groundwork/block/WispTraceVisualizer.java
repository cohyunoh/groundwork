package cnnr.groundwork.block;

import cnnr.groundwork.vision.BuildVisionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A faint always-visible hint at each nearby wisp when a player is NOT in Builder Vision, so an
 * intangible, otherwise-invisible cell never reads as a mystery obstruction. Deliberately subtle:
 * one soft particle per wisp, infrequent, only within a short radius of the player.
 */
public final class WispTraceVisualizer {
    private static final int RADIUS = 6;
    private static final double SPAWN_CHANCE = 0.5; // further thins the already-throttled tick

    private WispTraceVisualizer() {}

    public static void tick(MinecraftServer server) {
        BuildVisionService vision = BuildVisionService.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (vision.isActive(p.getUUID())) continue; // full marker already covers them

            ServerLevel level = p.level();
            BlockPos center = p.blockPosition();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!(level.getBlockEntity(pos) instanceof WispBlockEntity)) continue;
                        if (level.getRandom().nextFloat() >= SPAWN_CHANCE) continue;
                        level.sendParticles(p, ParticleTypes.WITCH, true, false,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }
        }
    }
}
