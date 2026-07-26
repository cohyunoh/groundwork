package cnnr.groundwork.client;

import cnnr.groundwork.Groundwork;
import cnnr.groundwork.network.PlanSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Draws a label for the single ghost block under the crosshair (Litematica-style contextual label). */
public final class GhostLabelHud {
    private GhostLabelHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Groundwork.id("ghost_label"),
            (guiGraphicsExtractor, deltaTracker) -> {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player == null || mc.level == null) return;

                // VERIFY: DeltaTracker#getGameTimeDeltaPartialTick(boolean) parameter meaning;
                // true is the conventional "current renderable partial tick" choice.
                float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
                Vec3 eye = player.getEyePosition(partialTick);
                Vec3 look = player.getViewVector(partialTick);

                PlanSyncPayload.Entry hit = ClientPlanStore.get().raycastGhost(eye, look, 6.0);
                if (hit == null) return;

                BlockState display = hit.displayState();
                String blockName = display.getBlock().getName().getString();
                int count = ClientPlanStore.get().countOfBlock(display.getBlock());
                String text = prefixFor(hit.changeType()) + blockName + suffixFor(hit.changeType(), count);

                int x = guiGraphicsExtractor.guiWidth() / 2;
                int y = guiGraphicsExtractor.guiHeight() / 2 + 12;
                guiGraphicsExtractor.centeredText(mc.font, text, x, y, colorFor(hit.changeType()));
            });
    }

    private static String prefixFor(byte changeType) {
        return switch (changeType) {
            case 0 -> "Place: ";
            case 1 -> "Clear: ";
            case 2 -> "Replace with: ";
            default -> "";
        };
    }

    private static String suffixFor(byte changeType, int count) {
        return changeType == 1 ? (" - " + count + " to clear") : (" - " + count + " needed");
    }

    private static int colorFor(byte changeType) {
        // GuiGraphicsExtractor#text silently no-ops when ARGB.alpha(color) == 0, so these need
        // an explicit opaque alpha byte (0xFF) or nothing draws at all.
        return switch (changeType) {
            case 0 -> 0xFF55FF55;
            case 1 -> 0xFFFF5555;
            case 2 -> 0xFF5599FF;
            default -> 0xFFFFFFFF;
        };
    }
}
