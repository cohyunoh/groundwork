package cnnr.groundwork.command;

import cnnr.groundwork.Groundwork;
import cnnr.groundwork.network.PlanSync;
import cnnr.groundwork.selection.CaptureService;
import cnnr.groundwork.selection.CaptureService.Plan;
import cnnr.groundwork.selection.Selection;
import cnnr.groundwork.selection.SelectionService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
}
