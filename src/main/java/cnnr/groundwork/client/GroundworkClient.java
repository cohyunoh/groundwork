package cnnr.groundwork.client;

import cnnr.groundwork.block.ModBlockEntities;
import cnnr.groundwork.block.ModBlocks;
import cnnr.groundwork.network.BuildVisionPayload;
import cnnr.groundwork.network.PlanSyncPayload;
import cnnr.groundwork.network.RegionEditPayload;
import cnnr.groundwork.network.WispSelectionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

public class GroundworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlanSyncPayload.TYPE, (payload, context) ->
            ClientPlanStore.get().accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(BuildVisionPayload.TYPE, (payload, context) ->
            ClientBuildVision.set(payload.active()));
        ClientPlayNetworking.registerGlobalReceiver(RegionEditPayload.TYPE, (payload, context) ->
            ClientRegionEdit.set(payload.editing(), payload.wispPos()));
        ClientPlayNetworking.registerGlobalReceiver(WispSelectionPayload.TYPE, (payload, context) ->
            ClientWispSelection.set(payload.hasSelection(), payload.pos()));

        BlockEntityRendererRegistry.register(ModBlockEntities.WISP, WispBlockEntityRenderer::new);

        // Vanilla's own "looking at this block" outline box is contextual by nature (only the
        // currently-targeted block), which is exactly the behavior we want: show it for a wisp
        // only when Builder Vision is on (i.e. only when it's actually interactable/breakable),
        // suppress it otherwise so an inert wisp never looks like a mystery obstruction.
        // getShape() stays a full cube so the block remains targetable/breakable at all.
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineState) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return true;
            if (mc.level.getBlockState(outlineState.pos()).getBlock() != ModBlocks.WISP_BLOCK) return true;
            return ClientBuildVision.isActive();
        });

        PlanRenderer.register();
        GhostLabelHud.register();
        RegionScrollHandler.register();
    }
}
