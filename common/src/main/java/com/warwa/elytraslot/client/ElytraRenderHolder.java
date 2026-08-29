package com.warwa.elytraslot.client;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface mixed into HumanoidRenderState: carries the elytra from this
 * mod's slot (BODY or an external host without native rendering) into the
 * render state each frame.
 */
public interface ElytraRenderHolder {

    ItemStack elytraslot$getElytra();

    void elytraslot$setElytra(ItemStack stack);
}
