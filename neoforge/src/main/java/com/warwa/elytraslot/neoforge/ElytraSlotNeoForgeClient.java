package com.warwa.elytraslot.neoforge;

import com.warwa.elytraslot.client.ElytraSlotConfigScreen;
import com.warwa.elytraslot.host.ElytraHosts;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only wiring, kept out of the main class so a dedicated server never classloads it. */
final class ElytraSlotNeoForgeClient {

    private ElytraSlotNeoForgeClient() {
    }

    static void init(ModContainer modContainer, Runnable registerGliderBehavior) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (container, parent) -> new ElytraSlotConfigScreen(parent));

        // On a remote server the client never fires ServerAboutToStartEvent, so the
        // glider scan also runs on join (one-shot, shared with the server path).
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) ->
            registerGliderBehavior.run());

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
            ElytraHosts.resetClient());
    }
}
