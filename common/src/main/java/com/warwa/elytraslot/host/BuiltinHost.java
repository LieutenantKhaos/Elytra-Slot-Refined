package com.warwa.elytraslot.host;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The default host: the elytra lives in the vanilla BODY equipment slot
 * (unused by players in vanilla). Because it is real equipment, vanilla itself
 * handles persistence, client sync, death drops, keepInventory, Curse of
 * Vanishing, attribute modifiers, enchantment iteration, and Mending — and any
 * third-party mod that reads live equipment (durability HUDs, gravestone mods
 * scanning inventory indices 0..42) sees it with no compat code.
 *
 * <p>The only vanilla gap is flight: {@code LivingEntity.canGlideUsing} requires
 * the item's equippable slot to match the slot it sits in, which our
 * BodyGliderMixin extends to accept chest-equippable gliders in BODY. Everything
 * else — glide activation, durability cadence, break effects — then runs through
 * untouched vanilla code.
 */
public final class BuiltinHost implements ElytraHost {

    public static final String ID = "builtin";

    BuiltinHost() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public ItemStack get(Player player) {
        return player.getItemBySlot(EquipmentSlot.BODY);
    }

    @Override
    public boolean set(Player player, ItemStack stack, boolean silent) {
        if (silent) {
            // Writes through the inventory view skip onEquipItem (no sound/game event);
            // the vanilla equipment tick diff still applies modifiers and syncs.
            player.getInventory().setItem(Inventory.SLOT_BODY_ARMOR, stack);
        } else {
            player.setItemSlot(EquipmentSlot.BODY, stack);
        }
        return true;
    }
}
