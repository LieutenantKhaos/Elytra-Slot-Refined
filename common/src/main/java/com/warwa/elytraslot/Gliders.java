package com.warwa.elytraslot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * The single definition of "elytra-like". Any item carrying the {@code minecraft:glider}
 * data component qualifies, which makes modded elytras work automatically.
 */
public final class Gliders {

    private Gliders() {
    }

    /** True for any item that can glide (vanilla or modded elytra). */
    public static boolean isGlider(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.GLIDER);
    }

    /**
     * True for gliders that equip to the CHEST slot — the only kind our slot accepts.
     * A glider equipping elsewhere (e.g. a modded HEAD glider) keeps its native behavior.
     */
    public static boolean isChestGlider(ItemStack stack) {
        if (!isGlider(stack)) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }
}
