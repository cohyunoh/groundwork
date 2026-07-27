package cnnr.groundwork.command;

import cnnr.groundwork.Groundwork;
import cnnr.groundwork.block.WispBlockEntity;
import cnnr.groundwork.network.PlanSync;
import cnnr.groundwork.selection.CaptureService;
import cnnr.groundwork.selection.CaptureService.Plan;
import cnnr.groundwork.selection.Selection;
import cnnr.groundwork.selection.SelectionService;
import cnnr.groundwork.vision.RegionEditService;
import cnnr.groundwork.vision.WispSelectionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GwCommands {
    private GwCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gw")
            .then(Commands.literal("wand").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                ItemStack wand = new ItemStack(Items.STICK);
                wand.set(DataComponents.CUSTOM_NAME, Component.literal("Groundwork Wand"));
                p.getInventory().add(wand);
                ctx.getSource().sendSuccess(() -> Component.literal("Got the Groundwork Wand. Left-click = corner A, right-click = corner B."), false);
                return 1;
            }))
            .then(Commands.literal("wisp").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                BlockPos pos = p.blockPosition();
                return Groundwork.placeWisp(p, pos) ? 1 : 0;
            }))
            .then(Commands.literal("corner")
                .then(Commands.literal("a").executes(ctx -> setRegionCorner(ctx, true)))
                .then(Commands.literal("b").executes(ctx -> setRegionCorner(ctx, false))))
            // Temporary manual test path for region-edit now that read-mode right-click selects
            // instead of editing (see the MIGRATION note in Groundwork.java). Operates on whatever
            // wisp is currently selected. Region-edit fully migrates into planning mode later.
            .then(Commands.literal("edit").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                MinecraftServer server = ctx.getSource().getServer();
                BlockPos wispPos = WispSelectionService.get(server).getSelected(p.getUUID());
                if (wispPos == null) {
                    ctx.getSource().sendFailure(Component.literal("Select a wisp first (right-click it with the Lens)."));
                    return 0;
                }
                Groundwork.enterRegionEdit(p, wispPos);
                return 1;
            }))
            .then(Commands.literal("capture")
                .then(Commands.literal("on").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    Selection sel = SelectionService.get(server).get(p.getUUID());
                    if (sel == null || !sel.isComplete()) {
                        ctx.getSource().sendFailure(Component.literal("Set both corners with the wand first."));
                        return 0;
                    }
                    int n = CaptureService.get(server).startCapture(server, p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("Capture ON: snapshotted " + n + " blocks.").withStyle(ChatFormatting.GREEN), false);
                    return 1;
                })))
            .then(Commands.literal("restore").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                MinecraftServer server = ctx.getSource().getServer();
                int n = CaptureService.get(server).restore(server, p.getUUID());
                ctx.getSource().sendSuccess(() -> Component.literal("Restored " + n + " blocks.").withStyle(ChatFormatting.AQUA), false);
                return 1;
            }))
            .then(Commands.literal("plan")
                .then(Commands.literal("save").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    CaptureService cap = CaptureService.get(server);
                    cap.savePlan(server, p.getUUID());
                    Plan plan = cap.getPlan(p.getUUID());
                    if (plan == null || plan.entries().isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("Nothing to save — capture on, build something, then save."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Plan saved: " + plan.addCount() + " to add, " + plan.removeCount() + " to clear, "
                        + plan.replaceCount() + " to replace (" + plan.changedCount() + " changes).")
                        .withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }))
                .then(Commands.literal("show").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    int n = CaptureService.get(server).showPlan(server, p.getUUID());
                    if (n == 0) {
                        ctx.getSource().sendFailure(Component.literal("No saved plan. Run /gw plan save first."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Showing plan: " + n + " blocks applied.").withStyle(ChatFormatting.AQUA), false);
                    return 1;
                }))
                .then(Commands.literal("hide").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    int n = CaptureService.get(server).hidePlan(server, p.getUUID());
                    if (n == 0) {
                        ctx.getSource().sendFailure(Component.literal("No saved plan. Run /gw plan save first."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Plan hidden: " + n + " blocks restored.").withStyle(ChatFormatting.AQUA), false);
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Plan plan = CaptureService.get(ctx.getSource().getServer()).getPlan(p.getUUID());
                    if (plan == null || plan.entries().isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No saved plan."), false);
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Plan: " + plan.addCount() + " add / " + plan.removeCount() + " clear / "
                        + plan.replaceCount() + " replace (" + plan.changedCount() + " changes)."), false);
                    return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    if (!CaptureService.get(server).isCapturing(p.getUUID())) {
                        ctx.getSource().sendFailure(Component.literal("No active planning session to cancel."));
                        return 0;
                    }
                    int restored = CaptureService.get(server).cancelCapture(server, p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Planning cancelled. Restored " + restored + " blocks; snapshot discarded."), false);
                    return 1;
                }))
                .then(Commands.literal("sync").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CaptureService cap = CaptureService.get(ctx.getSource().getServer());
                    Plan plan = cap.getPlan(p.getUUID());
                    if (plan == null || plan.entries().isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("No saved plan. Run /gw plan save first."));
                        return 0;
                    }
                    PlanSync.sendPlanToSelf(cap, p);
                    int n = plan.entries().size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Syncing plan to your client (" + n + " entries).").withStyle(ChatFormatting.AQUA), false);
                    return 1;
                })))
            .then(Commands.literal("status").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                MinecraftServer server = ctx.getSource().getServer();
                CaptureService cap = CaptureService.get(server);
                Selection sel = SelectionService.get(server).get(p.getUUID());
                boolean capturing = cap.isCapturing(p.getUUID());
                int count = cap.capturedCount(p.getUUID());
                String box = (sel != null && sel.isComplete())
                    ? (sel.volume() + " blocks") : "incomplete";
                boolean hasPlan = cap.hasPlan(p.getUUID());
                Plan plan = cap.getPlan(p.getUUID());
                String planStr = hasPlan
                    ? ("saved (" + plan.changedCount() + " changes: " + plan.addCount() + " add / "
                       + plan.removeCount() + " clear / " + plan.replaceCount() + " replace)")
                    : "none";
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Selection: " + box + " | capturing: " + capturing + " | snapshot: " + count + " blocks"
                    + " | plan: " + planStr), false);
                return 1;
            })));
    }

    private static int setRegionCorner(CommandContext<CommandSourceStack> ctx, boolean isA) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        BlockPos wispPos = RegionEditService.get(server).getEditing(p.getUUID());
        if (wispPos == null) {
            ctx.getSource().sendFailure(Component.literal("Right-click a wisp with the Lens first (in Builder Vision)."));
            return 0;
        }

        ServerLevel level = p.level();
        if (!(level.getBlockEntity(wispPos) instanceof WispBlockEntity wisp)) {
            ctx.getSource().sendFailure(Component.literal("That wisp is gone."));
            RegionEditService.get(server).stopEditing(p.getUUID());
            return 0;
        }

        BlockPos point = freeSpacePoint(p, level);
        Selection region = wisp.getRegion();
        if (isA) region.setCornerA(point, level.dimension());
        else region.setCornerB(point, level.dimension());
        wisp.setRegion(region);

        String label = isA ? "Corner A" : "Corner B";
        StringBuilder msg = new StringBuilder(label + " set: " + point.getX() + ", " + point.getY() + ", " + point.getZ());
        if (region.isComplete()) {
            BlockPos mn = region.min(), mx = region.max();
            msg.append("  |  region ").append(mx.getX() - mn.getX() + 1).append("x").append(mx.getY() - mn.getY() + 1)
               .append("x").append(mx.getZ() - mn.getZ() + 1).append(" (").append(region.volume()).append(" blocks)");
        }
        String finalMsg = msg.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(finalMsg).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    /** A point in world space along the player's look ray: the first block hit, or a fixed max
     *  range if nothing's there — so corners can land in open air (below floors, in open sky). */
    private static BlockPos freeSpacePoint(ServerPlayer p, ServerLevel level) {
        Vec3 eye = p.getEyePosition(1.0f);
        Vec3 look = p.getViewVector(1.0f);
        double range = 32.0;
        Vec3 to = eye.add(look.scale(range));

        ClipContext clipContext = new ClipContext(eye, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p);
        BlockHitResult hit = level.clip(clipContext);
        Vec3 point = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        return BlockPos.containing(point);
    }
}
