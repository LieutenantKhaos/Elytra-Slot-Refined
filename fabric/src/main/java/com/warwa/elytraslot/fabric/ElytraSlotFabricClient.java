package com.warwa.elytraslot.fabric;

import com.warwa.elytraslot.host.ElytraHosts;
import com.warwa.elytraslot.net.HostSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ElytraSlotFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Fabric play-payload handlers already run on the client main thread.
        ClientPlayNetworking.registerGlobalReceiver(HostSyncPayload.TYPE, (payload, context) -> {
            ElytraHosts.applyClientHost(payload.hostId());
            ElytraHosts.applyClientPanel(payload.showPanel());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ElytraHosts.resetClient());

        // On a remote server the client never starts one itself, so the glider scan
        // also runs on join. It cannot run at init: item default components are not
        // bound yet. One-shot, shared with the server-start path.
        if (ElytraSlotFabric.trinketsLoaded()) {
            ClientPlayConnectionEvents.INIT.register((handler, client) ->
                ElytraSlotFabric.registerTrinketsCallbacks());
        }
    }
}
