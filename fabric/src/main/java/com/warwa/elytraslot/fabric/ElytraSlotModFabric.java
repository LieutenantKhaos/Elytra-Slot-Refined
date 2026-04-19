package com.warwa.elytraslot.fabric;

import com.warwa.elytraslot.ElytraSlotConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ElytraSlotModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElytraSlotConstants.LOGGER.info("Elytra Slot mod loaded! (Fabric)");

        // C1 fix: detect Trinkets (Fabric analogue of NeoForge's Curios). If present,
        // warn about potential conflict with our custom elytra slot.
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] Trinkets mod detected — our custom elytra slot may conflict with Trinkets' back/body slot. "
                + "Double-render and double-damage are possible if both mods accept the same stack."
            );
        } else {
            ElytraSlotConstants.LOGGER.info("[elytraslot] Trinkets not detected — running standalone");
        }

        // F fix: register S2C payload type + broadcast dispatcher.
        ElytraSlotNetworkFabric.register();
    }
}
