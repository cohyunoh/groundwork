package cnnr.groundwork.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class CaptureService {
    // A saved plan: original + built state per position, with a derived change type.
    public record PlanEntry(BlockPos pos, BlockState original, BlockState built) {
        public ChangeType type() {
            boolean origAir = original.isAir();
            boolean builtAir = built.isAir();
            if (origAir && !builtAir) return ChangeType.ADD;
            if (!origAir && builtAir) return ChangeType.REMOVE;
            if (!original.equals(built)) return ChangeType.REPLACE;
            return ChangeType.NONE; // unchanged (e.g. placed then removed back to original)
        }
    }
    public enum ChangeType { ADD, REMOVE, REPLACE, NONE }

    // A whole plan for one player.
    public record Plan(List<PlanEntry> entries, ResourceKey<Level> dimension) {
        public int addCount()     { int n=0; for (var e: entries) if (e.type()==ChangeType.ADD) n++; return n; }
        public int removeCount()  { int n=0; for (var e: entries) if (e.type()==ChangeType.REMOVE) n++; return n; }
        public int replaceCount() { int n=0; for (var e: entries) if (e.type()==ChangeType.REPLACE) n++; return n; }
        public int changedCount() { int n=0; for (var e: entries) if (e.type()!=ChangeType.NONE) n++; return n; }
    }

    private static final WeakHashMap<MinecraftServer, CaptureService> INSTANCES = new WeakHashMap<>();
    public static CaptureService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new CaptureService());
    }

    private final Map<UUID, Map<BlockPos, BlockState>> originals = new HashMap<>();
    private final Set<UUID> capturing = new HashSet<>();
    private final Map<UUID, Plan> savedPlans = new HashMap<>();

    /** Snapshot every block currently in the player's selection as "original". Returns the count snapshotted. */
    public int startCapture(MinecraftServer server, UUID id) {
        Selection sel = SelectionService.get(server).get(id);
        if (sel == null || !sel.isComplete() || sel.getDimension() == null) return 0;
        ServerLevel level = server.getLevel(sel.getDimension());
        if (level == null) return 0;

        BlockPos min = sel.min(), max = sel.max();
        Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    snapshot.put(pos, level.getBlockState(pos));
                }
            }
        }
        originals.put(id, snapshot);
        capturing.add(id);
        return snapshot.size();
    }

    public boolean isCapturing(UUID id) { return capturing.contains(id); }
    public int capturedCount(UUID id) {
        Map<BlockPos, BlockState> m = originals.get(id);
        return m == null ? 0 : m.size();
    }

    /** Abandon the current session: restore the world to the snapshot, then discard it. Returns blocks restored. */
    public int cancelCapture(MinecraftServer server, UUID id) {
        int restored = restore(server, id);   // puts the world back to the pre-capture snapshot
        capturing.remove(id);                 // ensure we're no longer marked capturing
        originals.remove(id);                 // discard the snapshot (restore already clears it, but be explicit)
        return restored;
    }

    /** Restore captured originals for this player; returns count restored. */
    public int restore(MinecraftServer server, UUID id) {
        Map<BlockPos, BlockState> m = originals.get(id);
        if (m == null || m.isEmpty()) return 0;
        Selection sel = SelectionService.get(server).get(id);
        ServerLevel level = (sel != null && sel.getDimension() != null)
                ? server.getLevel(sel.getDimension()) : null;
        if (level == null) return 0;

        int n = 0;
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            level.setBlockAndUpdate(e.getKey(), e.getValue());
            n++;
        }
        m.clear();
        return n;
    }

    /** Snapshot the built state at every captured position -> produces the saved Plan. */
    public Plan buildPlan(MinecraftServer server, UUID id) {
        Map<BlockPos, BlockState> origs = originals.get(id);
        Selection sel = SelectionService.get(server).get(id);
        if (origs == null || origs.isEmpty() || sel == null || sel.getDimension() == null) {
            return new Plan(List.of(), sel != null ? sel.getDimension() : null);
        }
        ServerLevel level = server.getLevel(sel.getDimension());
        if (level == null) return new Plan(List.of(), sel.getDimension());

        List<PlanEntry> entries = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : origs.entrySet()) {
            BlockPos pos = e.getKey();
            BlockState original = e.getValue();
            BlockState built = level.getBlockState(pos); // current world = final build
            PlanEntry entry = new PlanEntry(pos, original, built);
            if (entry.type() != ChangeType.NONE) entries.add(entry); // drop no-op changes
        }
        return new Plan(entries, sel.getDimension());
    }

    public void savePlan(MinecraftServer server, UUID id) { savedPlans.put(id, buildPlan(server, id)); }
    public Plan getPlan(UUID id) { return savedPlans.get(id); }
    public boolean hasPlan(UUID id) { return savedPlans.containsKey(id) && !savedPlans.get(id).entries().isEmpty(); }

    /** Apply the saved plan's BUILT states into the world (real blocks). */
    public int showPlan(MinecraftServer server, UUID id) {
        Plan plan = savedPlans.get(id);
        if (plan == null || plan.entries().isEmpty() || plan.dimension() == null) return 0;
        ServerLevel level = server.getLevel(plan.dimension());
        if (level == null) return 0;

        int n = 0;
        for (PlanEntry e : plan.entries()) {
            level.setBlockAndUpdate(e.pos(), e.built());
            n++;
        }
        return n;
    }

    /** Apply the saved plan's ORIGINAL states into the world. Keeps the plan saved. */
    public int hidePlan(MinecraftServer server, UUID id) {
        Plan plan = savedPlans.get(id);
        if (plan == null || plan.entries().isEmpty() || plan.dimension() == null) return 0;
        ServerLevel level = server.getLevel(plan.dimension());
        if (level == null) return 0;

        int n = 0;
        for (PlanEntry e : plan.entries()) {
            level.setBlockAndUpdate(e.pos(), e.original());
            n++;
        }
        return n;
    }
}
