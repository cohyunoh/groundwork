package cnnr.groundwork.vision;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Tracks, per player, an in-progress SPIKE planning-dimension session: where to return them and
 *  what block range in the planning dimension holds their copy (so /gw plan back can clear it). */
public final class PlanningSessionService {
    private static final WeakHashMap<MinecraftServer, PlanningSessionService> INSTANCES = new WeakHashMap<>();

    public static PlanningSessionService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new PlanningSessionService());
    }

    public record Session(ResourceKey<Level> returnDim, Vec3 returnPos, float returnYaw, float returnPitch,
                           BlockPos copyMin, BlockPos copyMax) {}

    private final Map<UUID, Session> sessions = new HashMap<>();

    public void start(UUID id, Session session) { sessions.put(id, session); }
    public Session get(UUID id) { return sessions.get(id); }
    public void end(UUID id) { sessions.remove(id); }
}
