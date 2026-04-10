package com.warwa.elytraslot.neoforge;

import com.warwa.elytraslot.ElytraSlotConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ElytraSlotConstants.MOD_ID)
public class ElytraSlotModNeoForge {

    public ElytraSlotModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ElytraSlotConstants.LOGGER.info("Elytra Slot mod loaded! (NeoForge)");
    }
}
