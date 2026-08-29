package com.warwa.elytraslot.host;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface on Player holding a legacy 2.x elytra that could not be placed
 * on load (BODY occupied and inventory full). It is written back out under the
 * legacy NBT key so the migration simply retries on the next load instead of
 * destroying the item.
 */
public interface LegacyElytraCarrier {

    ItemStack elytraslot$getPendingLegacyElytra();

    void elytraslot$setPendingLegacyElytra(ItemStack stack);
}
