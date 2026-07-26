package cnnr.groundwork.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Debug-only particle outline of each player's current selection. Testing aid, not a real feature. */
public final class SelectionVisualizer {
    private static final double STEP = 0.5;
    private static final double EPS = 1e-6;

    private SelectionVisualizer() {}

    public static void tick(MinecraftServer server) {
        SelectionService sels = SelectionService.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Selection sel = sels.get(p.getUUID());
            if (sel == null) continue;
            if (sel.getDimension() == null || !sel.getDimension().equals(p.level().dimension())) continue;

            ServerLevel level = p.level();
            if (sel.isComplete()) {
                drawBox(level, p, sel);
            } else {
                if (sel.getCornerA() != null) drawPoint(level, p, sel.getCornerA());
                if (sel.getCornerB() != null) drawPoint(level, p, sel.getCornerB());
            }
        }
    }

    private static void drawPoint(ServerLevel level, ServerPlayer p, BlockPos pos) {
        spawn(level, p, ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void drawBox(ServerLevel level, ServerPlayer p, Selection sel) {
        BlockPos mn = sel.min();
        BlockPos mx = sel.max();
        double x0 = mn.getX(), y0 = mn.getY(), z0 = mn.getZ();
        double x1 = mx.getX() + 1.0, y1 = mx.getY() + 1.0, z1 = mx.getZ() + 1.0;

        for (double x = x0; x <= x1 + EPS; x += STEP) {
            spawn(level, p, ParticleTypes.END_ROD, x, y0, z0);
            spawn(level, p, ParticleTypes.END_ROD, x, y0, z1);
            spawn(level, p, ParticleTypes.END_ROD, x, y1, z0);
            spawn(level, p, ParticleTypes.END_ROD, x, y1, z1);
        }
        for (double y = y0; y <= y1 + EPS; y += STEP) {
            spawn(level, p, ParticleTypes.END_ROD, x0, y, z0);
            spawn(level, p, ParticleTypes.END_ROD, x0, y, z1);
            spawn(level, p, ParticleTypes.END_ROD, x1, y, z0);
            spawn(level, p, ParticleTypes.END_ROD, x1, y, z1);
        }
        for (double z = z0; z <= z1 + EPS; z += STEP) {
            spawn(level, p, ParticleTypes.END_ROD, x0, y0, z);
            spawn(level, p, ParticleTypes.END_ROD, x1, y0, z);
            spawn(level, p, ParticleTypes.END_ROD, x0, y1, z);
            spawn(level, p, ParticleTypes.END_ROD, x1, y1, z);
        }
    }

    private static void spawn(ServerLevel level, ServerPlayer p, net.minecraft.core.particles.SimpleParticleType type,
                               double x, double y, double z) {
        level.sendParticles(p, type, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
