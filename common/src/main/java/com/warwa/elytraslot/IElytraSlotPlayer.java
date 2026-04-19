package com.warwa.elytraslot;

import net.minecraft.world.item.ItemStack;

/**
 * Interface mixed into Player to provide persistent elytra slot storage.
 */
public interface IElytraSlotPlayer {
    ElytraSlotContainer elytraslot_getElytraContainer();

    default ItemStack elytraslot_getElytraStack() {
        return elytraslot_getElytraContainer().getItem(0);
    }

    /**
     * Writes the slot and fires the interactive equip effects (sound + game event).
     * Use for player-driven changes: right-click equip, shift-click, drag-drop.
     */
    default void elytraslot_setElytraStack(ItemStack stack) {
        elytraslot_getElytraContainer().setItem(0, stack);
    }

    /**
     * Writes the slot silently — no sound, no game event. Use for NBT load,
     * death-drop clearing, and respawn carry-over.
     */
    default void elytraslot_setElytraStackSilent(ItemStack stack) {
        elytraslot_getElytraContainer().setItemSilent(0, stack);
    }
}
