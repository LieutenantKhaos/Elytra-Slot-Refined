package com.warwa.elytraslot.fabric;

import com.warwa.elytraslot.ElytraSlotConstants;
import net.fabricmc.api.ModInitializer;

public class ElytraSlotModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ElytraSlotConstants.LOGGER.info("Elytra Slot mod loaded! (Fabric)");
    }
}
