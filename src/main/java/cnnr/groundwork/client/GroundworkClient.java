package cnnr.groundwork.client;

import cnnr.groundwork.network.PlanSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GroundworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlanSyncPayload.TYPE, (payload, context) ->
            ClientPlanStore.get().accept(payload));

        PlanRenderer.register();
        GhostLabelHud.register();
    }
}
