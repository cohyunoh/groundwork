package cnnr.groundwork.vision;

import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Tracks which players currently have build-vision (ghost rendering) toggled on. Session-only; not persisted. */
public final class BuildVisionService {
    private static final WeakHashMap<MinecraftServer, BuildVisionService> INSTANCES = new WeakHashMap<>();
    public static BuildVisionService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new BuildVisionService());
    }

    private final Set<UUID> active = new HashSet<>();

    public boolean isActive(UUID id) { return active.contains(id); }

    /** Toggle and return the new state. */
    public boolean toggle(UUID id) {
        if (active.remove(id)) return false;
        active.add(id);
        return true;
    }

    public void clear(UUID id) { active.remove(id); }
}
