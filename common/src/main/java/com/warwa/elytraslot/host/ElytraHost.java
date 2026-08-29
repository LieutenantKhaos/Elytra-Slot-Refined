package com.warwa.elytraslot.host;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Where the equipped elytra lives. Exactly one host is active per game:
 * builtin (vanilla BODY equipment slot), Trinkets, or Curios.
 *
 * <p>The capability flags declare what the host mod already does natively, so the
 * rest of this mod knows when to stand down. The builtin host manages nothing
 * itself — vanilla code paths cover it because the elytra is real equipment.
 * The Trinkets Updated fork natively implements flight, durability, mending,
 * enchantment iteration, rendering, and right-click equip for glider items in its
 * slots; Curios implements only storage/sync/mending, so this mod supplies flight,
 * enchantment iteration, and rendering in Curios mode.
 */
public interface ElytraHost {

    String id();

    /** The elytra currently stored in this host's slot, or EMPTY. */
    ItemStack get(Player player);

    /**
     * Writes the stack into this host's slot. Returns false if the slot does not
     * exist or the write did not take — callers must not destroy the previous
     * stack until this returns true.
     */
    boolean set(Player player, ItemStack stack, boolean silent);

    /**
     * Whether this menu slot is the host mod's elytra slot. Used to give that slot
     * the same forgiving surround as our own, so a near-miss with an item on the
     * cursor does nothing instead of throwing it on the ground.
     */
    default boolean isHostSlot(Slot slot) {
        return false;
    }

    /**
     * Whether this host's slot would accept the stack right now — the same gate a
     * manual drag into the host mod's own screen would have to pass. Callers must
     * check the slot is empty separately; {@link #set} overwrites.
     */
    default boolean canAccept(Player player, ItemStack stack) {
        return false;
    }

    /**
     * Applies glide durability damage to the stack in this host's slot, mirroring
     * vanilla's in-place hurtAndBreak. Only called for hosts that do not manage
     * flight themselves (today: Curios).
     */
    default void hurt(Player player, int amount) {
    }

    default boolean isBuiltin() {
        return false;
    }

    /** Host mod natively activates gliding and applies glide durability. */
    default boolean managesFlight() {
        return false;
    }

    /** Host mod natively feeds its stacks into enchantment equipment iteration. */
    default boolean managesEnchantmentEffects() {
        return false;
    }

    /** Host mod natively repairs its stacks from XP orbs (Mending). */
    default boolean managesXpRepair() {
        return false;
    }

    /**
     * Host mod natively renders this stack on the player's back. Asked per stack
     * because accessory mods key rendering to the item, not the slot — a modded
     * glider the host has no render asset for still needs our own wings layer.
     */
    default boolean managesRenderingOf(ItemStack stack) {
        return false;
    }

    /** Host mod natively handles right-click-to-equip from the hand. */
    default boolean managesUseEquip() {
        return false;
    }

    /** Host mod natively handles death drops for its slot. */
    default boolean managesDeathDrops() {
        return false;
    }
}
