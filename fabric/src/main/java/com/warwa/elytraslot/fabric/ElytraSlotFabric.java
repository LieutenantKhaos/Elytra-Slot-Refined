package com.warwa.elytraslot.fabric;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.compat.trinkets.TrinketsCompat;
import com.warwa.elytraslot.host.JoinHandling;
import com.warwa.elytraslot.net.HostSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public final class ElytraSlotFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        if (trinketsLoaded()) {
            try {
                TrinketsCompat.register();
            } catch (Throwable t) {
                ElytraSlot.LOGGER.error("Trinkets integration failed to initialize; using the built-in slot", t);
            }
        }

        ElytraSlot.init(new FabricPlatform());

        PayloadTypeRegistry.clientboundPlay().register(HostSyncPayload.TYPE, HostSyncPayload.STREAM_CODEC);

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
            JoinHandling.copyPendingLegacyElytra(oldPlayer, newPlayer));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // This mod adds a slot to the inventory menu, so a client without it
            // would receive 47 slots for a 46-slot menu and break on the first
            // inventory sync. Fabric cannot negotiate that away, so refuse the
            // connection with a readable reason instead.
            if (!ServerPlayNetworking.canSend(handler, HostSyncPayload.TYPE)) {
                // Literal, not a translation key: the client being kicked is by
                // definition the one without our language file.
                handler.disconnect(Component.literal(
                    "This server requires the Elytra Slot Refined mod."));
                return;
            }
            JoinHandling.sendHostSync(handler.player);
            // Deferred one tick so inventory NBT and accessory-mod data are loaded.
            server.execute(() -> JoinHandling.relocateStrandedElytra(handler.player));
        });

        // Glider callbacks need the item registry to be complete, so they are
        // (re-)registered when a server starts; the client entry point covers the
        // client side after every mod's main entry point has run.
        if (trinketsLoaded()) {
            ServerLifecycleEvents.SERVER_STARTING.register(server -> registerTrinketsCallbacks());
        }
    }

    static boolean trinketsLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("trinkets") || loader.isModLoaded("trinkets_updated");
    }

    static void registerTrinketsCallbacks() {
        try {
            TrinketsCompat.registerGliderCallbacks();
        } catch (Throwable t) {
            ElytraSlot.LOGGER.error("Failed to register Trinkets glider callbacks", t);
        }
    }
}
