package com.warwa.elytraslot;

import net.minecraft.world.item.ItemStack;

/**
 * Interface mixed into HumanoidRenderState to carry elytra data for rendering.
 */
public interface IElytraHolder {
    ItemStack elytraslot_getAccessoriesElytra();
    void elytraslot_setAccessoriesElytra(ItemStack stack);
}
