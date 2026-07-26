package cnnr.groundwork.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SelectionService {
    private static final WeakHashMap<MinecraftServer, SelectionService> INSTANCES = new WeakHashMap<>();
    public static SelectionService get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, s -> new SelectionService());
    }

    private final Map<UUID, Selection> selections = new HashMap<>();
    public Selection getOrCreate(UUID id) { return selections.computeIfAbsent(id, k -> new Selection()); }
    public Selection get(UUID id) { return selections.get(id); }

    public void setCornerA(ServerPlayer p, BlockPos pos) {
        Selection sel = getOrCreate(p.getUUID());
        sel.setCornerA(pos, p.level().dimension());
        feedback(p, sel, "Corner A", pos);
    }
    public void setCornerB(ServerPlayer p, BlockPos pos) {
        Selection sel = getOrCreate(p.getUUID());
        sel.setCornerB(pos, p.level().dimension());
        feedback(p, sel, "Corner B", pos);
    }
    private void feedback(ServerPlayer p, Selection sel, String label, BlockPos pos) {
        String msg = label + " set: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        if (sel.isComplete()) {
            BlockPos mn = sel.min(), mx = sel.max();
            msg += "  |  box " + (mx.getX()-mn.getX()+1) + "x" + (mx.getY()-mn.getY()+1)
                 + "x" + (mx.getZ()-mn.getZ()+1) + " (" + sel.volume() + " blocks)";
        }
        p.sendSystemMessage(Component.literal(msg), true);
    }
}
