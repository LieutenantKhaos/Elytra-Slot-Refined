package com.warwa.elytraslot.host;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/** Finds the mod's slot in a menu by class identity. */
public final class PanelSlots {

    private PanelSlots() {
    }

    @Nullable
    public static ElytraArmorSlot find(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot instanceof ElytraArmorSlot elytraSlot) {
                return elytraSlot;
            }
        }
        return null;
    }
}
