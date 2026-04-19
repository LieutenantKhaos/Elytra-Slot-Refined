package com.warwa.elytraslot.fabric;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotSyncPayload;
import com.warwa.elytraslot.client.ElytraSlotSyncClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Fabric client entry point. Registers the S2C receiver for
 * {@link ElytraSlotSyncPayload} so remote-player elytra visuals stay in sync (F fix).
 *
 * <p>The receiver must be registered on the client after the payload type itself is
 * registered in {@code ElytraSlotNetworkFabric} (which runs at main init, before the
 * client entry point). Both run before connection to any world.
 */
public class ElytraSlotModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ElytraSlotConstants.LOGGER.info("[elytraslot] Fabric client init BEGIN");

        ClientPlayNetworking.registerGlobalReceiver(
            ElytraSlotSyncPayload.TYPE,
            (payload, ctx) -> {
                // Fabric handler callback fires on the client thread.
                ElytraSlotConstants.LOGGER.info(
                    "[elytraslot] Fabric S2C receive playerId={} stack={}",
                    payload.playerId(), payload.stack()
                );
                ElytraSlotSyncClient.handle(payload);
            }
        );
        ElytraSlotConstants.LOGGER.info("[elytraslot] Fabric S2C receiver registered for {}",
            ElytraSlotSyncPayload.TYPE.id());

        ElytraSlotConstants.LOGGER.info("[elytraslot] Fabric client init END");
    }
}
