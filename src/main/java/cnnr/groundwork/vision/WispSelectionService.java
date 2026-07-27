package cnnr.groundwork.vision;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Tracks which wisp (by position) each player has focused in Builder Vision's read mode. Session-only. */
public final class WispSelectionService {
    private static final WeakHashMap<MinecraftServer, WispSelectionService> INSTANCES = new WeakHashMap<>();
    public static WispSelectionService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new WispSelectionService());
    }

    private final Map<UUID, BlockPos> selected = new HashMap<>();

    /** Selects the wisp at pos; re-selecting the already-selected wisp clears it instead.
     *  Returns the new selection, or null if the selection was cleared. */
    public BlockPos select(UUID id, BlockPos pos) {
        if (pos.equals(selected.get(id))) {
            selected.remove(id);
            return null;
        }
        selected.put(id, pos);
        return pos;
    }

    public BlockPos getSelected(UUID id) { return selected.get(id); }
    public void clear(UUID id) { selected.remove(id); }
}
