package cnnr.groundwork.client;

import cnnr.groundwork.network.PlanSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-side reassembly of received plans, rendered by {@link PlanRenderer} and labeled by {@link GhostLabelHud}. */
public final class ClientPlanStore {
    private static final ClientPlanStore INSTANCE = new ClientPlanStore();
    public static ClientPlanStore get() { return INSTANCE; }

    // owner -> received entries (reassembled across batches)
    private final Map<UUID, List<PlanSyncPayload.Entry>> plans = new HashMap<>();
    private Map<BlockPos, PlanSyncPayload.Entry> byPos = new HashMap<>();

    public void accept(PlanSyncPayload payload) {
        List<PlanSyncPayload.Entry> list = plans.computeIfAbsent(payload.owner(), k -> new ArrayList<>());
        if (payload.first()) list.clear();
        list.addAll(payload.entries());
        // when last==true the plan is complete; nothing else needed for 3a
        rebuildByPos();
    }

    private void rebuildByPos() {
        Map<BlockPos, PlanSyncPayload.Entry> map = new HashMap<>();
        for (List<PlanSyncPayload.Entry> entries : plans.values())
            for (PlanSyncPayload.Entry e : entries)
                map.put(e.pos(), e);
        byPos = map;
    }

    public Map<UUID, List<PlanSyncPayload.Entry>> getPlans() { return plans; }

    /** Steps along the look ray in 0.1-block increments up to maxDist, returns the first plan entry hit. */
    public PlanSyncPayload.Entry raycastGhost(Vec3 eye, Vec3 look, double maxDist) {
        for (double d = 0; d <= maxDist; d += 0.1) {
            BlockPos bp = BlockPos.containing(eye.x + look.x * d, eye.y + look.y * d, eye.z + look.z * d);
            PlanSyncPayload.Entry hit = byPos.get(bp);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Count of plan entries whose display block matches, for a materials-style count in the label. */
    public int countOfBlock(Block block) {
        int n = 0;
        for (List<PlanSyncPayload.Entry> entries : plans.values())
            for (PlanSyncPayload.Entry e : entries)
                if (e.displayState().getBlock() == block) n++;
        return n;
    }
}
