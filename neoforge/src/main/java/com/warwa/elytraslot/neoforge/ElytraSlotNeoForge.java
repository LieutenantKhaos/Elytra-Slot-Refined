package com.warwa.elytraslot.neoforge;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.compat.trinkets.TrinketsCompat;
import com.warwa.elytraslot.host.ElytraHosts;
import com.warwa.elytraslot.host.JoinHandling;
import com.warwa.elytraslot.net.HostSyncPayload;
import com.warwa.elytraslot.neoforge.compat.curios.CuriosCompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ElytraSlot.MOD_ID)
public final class ElytraSlotNeoForge {

    public ElytraSlotNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        boolean trinkets = isLoaded("trinkets") || isLoaded("trinkets_updated");
        boolean curios = isLoaded("curios");

        // Registration order defines auto-priority: Trinkets first (matches the
        // behavior of earlier versions for existing worlds), then Curios.
        if (trinkets) {
            try {
                TrinketsCompat.register();
            } catch (Throwable t) {
                ElytraSlot.LOGGER.error("Trinkets integration failed to initialize", t);
            }
        }
        if (curios) {
            try {
                CuriosCompat.register();
            } catch (Throwable t) {
                ElytraSlot.LOGGER.error("Curios integration failed to initialize", t);
            }
        }

        ElytraSlot.init(new NeoForgePlatform());

        modEventBus.addListener(this::onRegisterPayloads);

        // Item default components are not bound during mod loading, so the glider
        // scan runs at the latest point that still precedes any gameplay: server
        // start, and (for a remote client) joining a world. The scan is one-shot.
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) ->
            registerGliderBehavior(trinkets, curios));

        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event) ->
            JoinHandling.copyPendingLegacyElytra(event.getOriginal(), event.getEntity()));

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                JoinHandling.sendHostSync(player);
                MinecraftServer server = player.level().getServer();
                if (server != null) {
                    // Deferred one tick so inventory NBT and accessory data are loaded.
                    server.execute(() -> JoinHandling.relocateStrandedElytra(player));
                }
            }
        });

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ElytraSlotNeoForgeClient.init(modContainer, () -> registerGliderBehavior(trinkets, curios));
        }
    }

    /** One-shot; safe to call from several lifecycle points. */
    static void registerGliderBehavior(boolean trinkets, boolean curios) {
        if (trinkets) {
            try {
                TrinketsCompat.registerGliderCallbacks();
            } catch (Throwable t) {
                ElytraSlot.LOGGER.error("Failed to register Trinkets glider callbacks", t);
            }
        }
        if (curios) {
            try {
                CuriosCompat.registerGliderCurios();
            } catch (Throwable t) {
                ElytraSlot.LOGGER.error("Failed to register Curios glider behavior", t);
            }
        }
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // Deliberately NOT optional: this mod adds a slot to the inventory menu, so a
        // client without it would receive 47 slots for a 46-slot menu and break. A
        // required channel makes NeoForge reject such clients with a clear message
        // instead, and keeps the login-time send from throwing.
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(HostSyncPayload.TYPE, HostSyncPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
                ElytraHosts.applyClientHost(payload.hostId());
                ElytraHosts.applyClientPanel(payload.showPanel());
            }));
    }

    private static boolean isLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }
}
