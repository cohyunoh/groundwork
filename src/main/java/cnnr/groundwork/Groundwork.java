package cnnr.groundwork;

import cnnr.groundwork.block.ModBlockEntities;
import cnnr.groundwork.block.ModBlocks;
import cnnr.groundwork.block.WispBlockEntity;
import cnnr.groundwork.block.WispTraceVisualizer;
import cnnr.groundwork.command.GwCommands;
import cnnr.groundwork.item.ModItems;
import cnnr.groundwork.network.BuildVisionPayload;
import cnnr.groundwork.network.PlanSyncPayload;
import cnnr.groundwork.network.PushRegionFacePayload;
import cnnr.groundwork.network.RegionEditPayload;
import cnnr.groundwork.network.WispSelectionPayload;
import cnnr.groundwork.selection.Selection;
import cnnr.groundwork.selection.SelectionService;
import cnnr.groundwork.selection.SelectionVisualizer;
import cnnr.groundwork.vision.BuildVisionService;
import cnnr.groundwork.vision.RegionEditService;
import cnnr.groundwork.vision.WispSelectionService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Groundwork implements ModInitializer {
	public static final String MOD_ID = "groundwork";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Component WAND_NAME = Component.literal("Groundwork Wand");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Groundwork initializing (selection/capture/restore spike)");

		ModItems.register();
		ModBlocks.register();
		ModBlockEntities.register();

		PayloadTypeRegistry.clientboundPlay().register(PlanSyncPayload.TYPE, PlanSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BuildVisionPayload.TYPE, BuildVisionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RegionEditPayload.TYPE, RegionEditPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WispSelectionPayload.TYPE, WispSelectionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PushRegionFacePayload.TYPE, PushRegionFacePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PushRegionFacePayload.TYPE, (payload, context) ->
				pushFace(context.player(), payload.face(), payload.delta()));

		// Right-click block with wand -> corner B; right-click block with the Lens -> toggle build-vision
		// (sneak + right-click block with the Lens -> place a wisp there instead)
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer sp) {
				ItemStack held = sp.getItemInHand(hand);
				if (held.is(ModItems.SURVEYORS_LENS)) {
					if (sp.isShiftKeyDown()) {
						placeWisp(sp, hitResult.getBlockPos().relative(hitResult.getDirection()));
					} else {
						BlockPos clicked = hitResult.getBlockPos();
						boolean clickedWisp = sp.level().getBlockState(clicked).getBlock() == ModBlocks.WISP_BLOCK;
						boolean inVision = BuildVisionService.get(sp.level().getServer()).isActive(sp.getUUID());
						if (clickedWisp && inVision) {
							// MIGRATION: read-mode right-click used to enter region-edit directly;
							// it now selects the wisp for read-mode focus instead. Region editing
							// will live in planning mode later. To still test edits, select a wisp
							// here, then run "/gw edit" (uses RegionEditService/enterRegionEdit).
							selectWisp(sp, clicked);
						} else {
							toggleBuildVision(sp);
						}
					}
					return InteractionResult.SUCCESS;
				}
				if (isWand(held)) {
					SelectionService.get(sp.level().getServer()).setCornerB(sp, hitResult.getBlockPos());
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// Left-click (attack) block with wand -> corner A
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer sp) {
				ItemStack held = sp.getItemInHand(hand);
				if (isWand(held)) {
					SelectionService.get(sp.level().getServer()).setCornerA(sp, pos);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// Right-click in air with the Lens -> toggle build-vision
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer sp) {
				ItemStack held = sp.getItemInHand(hand);
				if (held.is(ModItems.SURVEYORS_LENS)) {
					toggleBuildVision(sp);
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// Build-vision is session-only: clear it on logout so a reconnect always starts OFF.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			BuildVisionService.get(server).clear(handler.player.getUUID());
			RegionEditService.get(server).stopEditing(handler.player.getUUID());
			WispSelectionService.get(server).clear(handler.player.getUUID());
		});

		// A wisp can only be removed while wearing Builder Vision -- retiring it is a deliberate ritual.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (state.getBlock() != ModBlocks.WISP_BLOCK) return true;
			if (world.isClientSide()) return true; // let the server decide
			if (player instanceof ServerPlayer sp) {
				boolean inVision = BuildVisionService.get(sp.level().getServer()).isActive(sp.getUUID());
				if (!inVision) {
					sp.sendOverlayMessage(Component.literal("Put on the Surveyor's Lens to remove a wisp.")
							.withStyle(ChatFormatting.GRAY));
					return false;
				}
			}
			return true;
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				GwCommands.register(dispatcher));

		// Debug-only particle outline of each player's selection, throttled to 4x/sec.
		int[] tickCounter = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = tickCounter[0]++;
			if (tick % 5 == 0) {
				SelectionVisualizer.tick(server);
			}
			if (tick % 30 == 0) {
				WispTraceVisualizer.tick(server);
			}
		});
	}

	/** Places a wisp at {@code pos} if the space is free; returns true if one was placed. */
	public static boolean placeWisp(ServerPlayer sp, BlockPos pos) {
		var world = sp.level();
		if (!world.getBlockState(pos).canBeReplaced()) {
			sp.sendOverlayMessage(Component.literal("Can't place a wisp there.").withStyle(ChatFormatting.RED));
			return false;
		}
		world.setBlockAndUpdate(pos, ModBlocks.WISP_BLOCK.defaultBlockState());
		if (world.getBlockEntity(pos) instanceof WispBlockEntity be) {
			be.setCreatedTick(world.getGameTime());
		}
		sp.sendOverlayMessage(Component.literal("Wisp established.").withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	/** Manual test path for region-edit (see the MIGRATION note above) — used by "/gw edit". */
	public static void enterRegionEdit(ServerPlayer sp, BlockPos wispPos) {
		RegionEditService.get(sp.level().getServer()).startEditing(sp.getUUID(), wispPos);
		ServerPlayNetworking.send(sp, new RegionEditPayload(true, wispPos));
		sp.sendOverlayMessage(Component.literal("Editing wisp region — set two corners (/gw corner a, /gw corner b).")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Read-mode focus: selects the wisp for the read-mode HUD (its region box + emphasized marker,
	 *  others hidden). Re-selecting the same wisp deselects it. */
	private static void selectWisp(ServerPlayer sp, BlockPos wispPos) {
		BlockPos now = WispSelectionService.get(sp.level().getServer()).select(sp.getUUID(), wispPos);
		ServerPlayNetworking.send(sp, new WispSelectionPayload(now != null, now != null ? now : BlockPos.ZERO));
		sp.sendOverlayMessage(Component.literal(now != null ? "Wisp selected." : "Wisp deselected.")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Pushes (delta=+1, outward) or pulls (delta=-1, inward) one face of the region the player is
	 *  currently editing. Ignored if the player isn't actually editing in Builder Vision (stale/
	 *  spoofed packet). Clamps to a minimum 1-block thickness on the moved axis so the box can't invert. */
	private static void pushFace(ServerPlayer sp, Direction face, int delta) {
		ServerLevel level = sp.level();
		BlockPos wispPos = RegionEditService.get(level.getServer()).getEditing(sp.getUUID());
		if (wispPos == null) return;
		if (!BuildVisionService.get(level.getServer()).isActive(sp.getUUID())) return;
		if (!(level.getBlockEntity(wispPos) instanceof WispBlockEntity wisp) || !wisp.hasRegion()) return;

		Selection region = wisp.getRegion();
		BlockPos min = region.min();
		BlockPos max = region.max();
		int minX = min.getX(), minY = min.getY(), minZ = min.getZ();
		int maxX = max.getX(), maxY = max.getY(), maxZ = max.getZ();

		switch (face) {
			case UP -> maxY = Math.max(maxY + delta, minY);
			case DOWN -> minY = Math.min(minY - delta, maxY);
			case EAST -> maxX = Math.max(maxX + delta, minX);
			case WEST -> minX = Math.min(minX - delta, maxX);
			case SOUTH -> maxZ = Math.max(maxZ + delta, minZ);
			case NORTH -> minZ = Math.min(minZ - delta, maxZ);
		}

		Selection newRegion = new Selection();
		newRegion.setCornerA(new BlockPos(minX, minY, minZ), region.getDimension());
		newRegion.setCornerB(new BlockPos(maxX, maxY, maxZ), region.getDimension());
		wisp.setRegion(newRegion);

		sp.sendOverlayMessage(Component.literal(
				"Region: " + (maxX - minX + 1) + " x " + (maxY - minY + 1) + " x " + (maxZ - minZ + 1))
				.withStyle(ChatFormatting.AQUA));
	}

	private static void toggleBuildVision(ServerPlayer sp) {
		MinecraftServer server = sp.level().getServer();
		boolean now = BuildVisionService.get(server).toggle(sp.getUUID());
		ServerPlayNetworking.send(sp, new BuildVisionPayload(now));
		if (!now) {
			// Selection/focus is a Builder-Vision (read-mode) concept; clear it when leaving vision.
			WispSelectionService.get(server).clear(sp.getUUID());
			ServerPlayNetworking.send(sp, new WispSelectionPayload(false, BlockPos.ZERO));
		}
		sp.sendOverlayMessage(Component.literal(now ? "Build-vision ON" : "Build-vision OFF")
				.withStyle(now ? ChatFormatting.AQUA : ChatFormatting.GRAY));
	}

	private static boolean isWand(ItemStack stack) {
		// VERIFY: ItemStack#is(Item) is a long-standing stable vanilla method; not directly
		// confirmed against the 26.2 mapped jar during research, but extremely unlikely to have changed.
		return stack.is(Items.STICK) && WAND_NAME.equals(stack.get(DataComponents.CUSTOM_NAME));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
