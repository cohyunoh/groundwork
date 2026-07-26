package cnnr.groundwork.network;

import cnnr.groundwork.selection.CaptureService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlanSync {
    private PlanSync() {}
    private static final int BATCH = 2000; // entries per packet; keep well under the ~1 MiB payload cap
    private static int nextPlanId = 1;

    /** Send `owner`'s saved plan to the given recipients. (One recipient now; many later for collaboration.) */
    public static void sendPlan(CaptureService svc, UUID owner, List<ServerPlayer> recipients) {
        CaptureService.Plan plan = svc.getPlan(owner);
        if (plan == null) return;
        List<CaptureService.PlanEntry> src = plan.entries();
        int planId = nextPlanId++;

        if (src.isEmpty()) {
            PlanSyncPayload empty = new PlanSyncPayload(owner, planId, true, true, List.of());
            for (ServerPlayer p : recipients) ServerPlayNetworking.send(p, empty);
            return;
        }
        for (int start = 0; start < src.size(); start += BATCH) {
            int end = Math.min(start + BATCH, src.size());
            boolean first = (start == 0);
            boolean last = (end == src.size());
            List<PlanSyncPayload.Entry> batch = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                CaptureService.PlanEntry e = src.get(i);
                batch.add(new PlanSyncPayload.Entry(e.pos(), (byte) e.type().ordinal(), e.original(), e.built()));
            }
            PlanSyncPayload payload = new PlanSyncPayload(owner, planId, first, last, batch);
            for (ServerPlayer p : recipients) ServerPlayNetworking.send(p, payload);
        }
    }

    /** Convenience: send an owner's plan to just themselves (the single-viewer case). */
    public static void sendPlanToSelf(CaptureService svc, ServerPlayer owner) {
        sendPlan(svc, owner.getUUID(), List.of(owner));
    }
}
