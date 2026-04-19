package com.warwa.elytraslot;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * One candidate target for per-tick elytra durability damage.
 *
 * Lives in the main mod package (not in {@code com.warwa.elytraslot.mixin}) because
 * mixin-transformed code cannot reference classes defined inside a mixin package.
 */
public record GliderSource(ItemStack stack, EquipmentSlot slot, boolean fromCustomSlot) {}
