package cnnr.groundwork.client;

import net.minecraft.core.BlockPos;

/** Client-side mirror of the server's per-player region-edit state, set from {@code RegionEditPayload}. */
public final class ClientRegionEdit {
    private static boolean editing = false;
    private static BlockPos wispPos = null;

    private ClientRegionEdit() {}

    public static boolean isEditing() { return editing; }
    public static BlockPos getWispPos() { return wispPos; }

    public static void set(boolean e, BlockPos pos) {
        editing = e;
        wispPos = pos;
    }
}
