package com.warwa.elytraslot;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Interface mixed into Player to provide persistent elytra slot storage.
 */
public interface IElytraSlotPlayer {
    SimpleContainer elytraslot_getElytraContainer();

    default ItemStack elytraslot_getElytraStack() {
        return elytraslot_getElytraContainer().getItem(0);
    }

    default void elytraslot_setElytraStack(ItemStack stack) {
        elytraslot_getElytraContainer().setItem(0, stack);
    }
}
