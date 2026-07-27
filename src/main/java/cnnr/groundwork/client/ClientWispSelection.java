package cnnr.groundwork.client;

import net.minecraft.core.BlockPos;

/** Client-side mirror of the server's per-player selected wisp, set from {@code WispSelectionPayload}. */
public final class ClientWispSelection {
    private static BlockPos selected = null;

    private ClientWispSelection() {}

    public static BlockPos getSelected() { return selected; }

    public static void set(boolean hasSelection, BlockPos pos) {
        selected = hasSelection ? pos : null;
    }
}
