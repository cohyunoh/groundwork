package cnnr.groundwork.client;

import cnnr.groundwork.network.PlanSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Draws each received plan as translucent color-coded ghost boxes via the 26.2 Gizmos API. */
public final class PlanRenderer {
    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;

    private PlanRenderer() {}

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            Map<UUID, List<PlanSyncPayload.Entry>> plans = ClientPlanStore.get().getPlans();
            if (plans.isEmpty()) return;

            Camera camera = context.gameRenderer().mainCamera();
            Vec3 cam = camera.position();

            for (List<PlanSyncPayload.Entry> entries : plans.values()) {
                for (PlanSyncPayload.Entry e : entries) {
                    BlockPos pos = e.pos();
                    double dx = pos.getX() + 0.5 - cam.x;
                    double dy = pos.getY() + 0.5 - cam.y;
                    double dz = pos.getZ() + 0.5 - cam.z;
                    if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQ) continue;

                    Gizmos.cuboid(new AABB(pos), styleFor(e.changeType()));
                }
            }
        });
    }

    private static GizmoStyle styleFor(byte changeType) {
        // ADD=0 green, REMOVE=1 red, REPLACE=2 blue/cyan; ~25% alpha translucent fill
        return switch (changeType) {
            case 0 -> GizmoStyle.fill(ARGB.color(64, 60, 255, 80));
            case 1 -> GizmoStyle.fill(ARGB.color(64, 255, 60, 60));
            case 2 -> GizmoStyle.fill(ARGB.color(64, 70, 150, 255));
            default -> GizmoStyle.fill(ARGB.color(50, 255, 255, 255));
        };
    }
}
