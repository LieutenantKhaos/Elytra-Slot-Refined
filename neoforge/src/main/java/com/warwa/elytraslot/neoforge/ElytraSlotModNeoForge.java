package com.warwa.elytraslot.neoforge;

import com.warwa.elytraslot.ElytraSlotConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(ElytraSlotConstants.MOD_ID)
public class ElytraSlotModNeoForge {

    public ElytraSlotModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ElytraSlotConstants.LOGGER.info("Elytra Slot mod loaded! (NeoForge)");

        // C1 fix: detect Curios. If present, warn about potential conflict.
        if (ModList.get() != null && ModList.get().isLoaded("curios")) {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] Curios mod detected — our custom elytra slot may conflict with Curios' back/body slot. "
                + "Double-render and double-damage are possible if both mods accept the same stack."
            );
        } else {
            ElytraSlotConstants.LOGGER.info("[elytraslot] Curios not detected — running standalone");
        }

        // F fix: register S2C payload type + broadcast dispatcher.
        ElytraSlotNetworkNeoForge.register(modEventBus);
    }
}
