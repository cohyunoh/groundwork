package cnnr.groundwork.client;

import cnnr.groundwork.block.ModBlockEntities;
import cnnr.groundwork.network.BuildVisionPayload;
import cnnr.groundwork.network.PlanSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class GroundworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlanSyncPayload.TYPE, (payload, context) ->
            ClientPlanStore.get().accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(BuildVisionPayload.TYPE, (payload, context) ->
            ClientBuildVision.set(payload.active()));

        BlockEntityRendererRegistry.register(ModBlockEntities.WISP, WispBlockEntityRenderer::new);

        PlanRenderer.register();
        GhostLabelHud.register();
    }
}
