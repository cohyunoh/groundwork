package cnnr.groundwork.client;

import cnnr.groundwork.block.WispBlockEntity;
import cnnr.groundwork.network.PushRegionFacePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientHotbarScrollEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hijacks hotbar scroll to push/pull a region face when editing a wisp and aiming at its box.
 *  Any other time, scroll is left alone (returns true = allow the normal hotbar change). */
public final class RegionScrollHandler {
    private static final double AIM_MAX_DIST = 64.0;

    private RegionScrollHandler() {}

    public static void register() {
        ClientHotbarScrollEvents.ALLOW.register(RegionScrollHandler::onScroll);
    }

    private static boolean onScroll(Inventory inventory, int oldSlot, int newSlot, double xOffset, double yOffset) {
        if (yOffset == 0.0) return true;
        if (!ClientBuildVision.isActive() || !ClientRegionEdit.isEditing()) return true;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        BlockPos wispPos = ClientRegionEdit.getWispPos();
        if (mc.level == null || player == null || wispPos == null) return true;
        if (!(mc.level.getBlockEntity(wispPos) instanceof WispBlockEntity wisp) || !wisp.hasRegion()) return true;

        BlockPos min = wisp.getRegion().min();
        BlockPos max = wisp.getRegion().max();
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Direction face = RegionFaceTargeter.aimedFace(eye, look, box, AIM_MAX_DIST);
        if (face == null) return true; // not aiming at the box: normal hotbar scroll

        // "Grow" (delta=+1) always means the face moves away from the box's own center. Whether
        // that reads as the surface moving toward or away from YOUR eye depends on which side of
        // the box you're standing on: from outside, growing a face pulls it closer to you (it
        // extends toward where you're standing); from inside, growing a face pushes it farther
        // away. Flip the scroll sign when outside so "scroll up" always feels like "push away
        // from me" regardless of vantage point.
        boolean insideBox = box.contains(eye);
        int scrollSign = yOffset > 0.0 ? 1 : -1;
        int delta = insideBox ? scrollSign : -scrollSign;
        ClientPlayNetworking.send(new PushRegionFacePayload(face, delta));
        return false; // consumed: don't also change the hotbar slot
    }
}
