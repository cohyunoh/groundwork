package cnnr.groundwork.vision;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Tracks which wisp (by position) each player is currently editing the region of. Session-only. */
public final class RegionEditService {
    private static final WeakHashMap<MinecraftServer, RegionEditService> INSTANCES = new WeakHashMap<>();
    public static RegionEditService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new RegionEditService());
    }

    private final Map<UUID, BlockPos> editing = new HashMap<>();

    public void startEditing(UUID id, BlockPos wispPos) { editing.put(id, wispPos); }
    public BlockPos getEditing(UUID id) { return editing.get(id); }
    public void stopEditing(UUID id) { editing.remove(id); }
}
