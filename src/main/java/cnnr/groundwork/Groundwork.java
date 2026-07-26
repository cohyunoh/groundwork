package cnnr.groundwork;

import cnnr.groundwork.command.GwCommands;
import cnnr.groundwork.network.PlanSyncPayload;
import cnnr.groundwork.selection.SelectionService;
import cnnr.groundwork.selection.SelectionVisualizer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

		PayloadTypeRegistry.clientboundPlay().register(PlanSyncPayload.TYPE, PlanSyncPayload.CODEC);

		// Right-click block with wand -> corner B
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer sp) {
				ItemStack held = sp.getItemInHand(hand);
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

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				GwCommands.register(dispatcher));

		// Debug-only particle outline of each player's selection, throttled to 4x/sec.
		int[] tickCounter = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (tickCounter[0]++ % 5 == 0) {
				SelectionVisualizer.tick(server);
			}
		});
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
