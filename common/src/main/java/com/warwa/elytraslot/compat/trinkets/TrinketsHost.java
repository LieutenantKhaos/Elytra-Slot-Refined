package com.warwa.elytraslot.compat.trinkets;

import com.warwa.elytraslot.host.ElytraHost;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.TrinketSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the elytra in the Trinkets Updated {@code chest/elytra} slot (registered
 * by this mod's data files). The Trinkets fork natively implements flight,
 * durability, mending, enchantment iteration, rendering, right-click equip, and
 * death drops for glider items in its slots, so every capability flag is true and
 * the rest of this mod stands down while this host is active.
 *
 * <p>Only classloaded when Trinkets is installed.
 */
public final class TrinketsHost implements ElytraHost {

    public static final TrinketsHost INSTANCE = new TrinketsHost();
    public static final String ID = "trinkets";
    public static final String SLOT_ID = "chest/elytra";

    /** The tag Trinkets' bundled wings render asset targets alongside the vanilla elytra. */
    private static final TagKey<Item> COMMON_ELYTRA_TAG =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "elytra"));

    private TrinketsHost() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ItemStack get(Player player) {
        TrinketSlotAccess access = access(player);
        return access != null ? access.get() : ItemStack.EMPTY;
    }

    @Override
    public boolean set(Player player, ItemStack stack, boolean silent) {
        TrinketSlotAccess access = access(player);
        return access != null && access.set(stack);
    }

    @Override
    public boolean isHostSlot(Slot slot) {
        return slot instanceof TrinketSlot trinketSlot
            && SLOT_ID.equals(trinketSlot.getType().getId());
    }

    @Override
    public boolean canAccept(Player player, ItemStack stack) {
        TrinketSlotAccess access = access(player);
        return access != null && access.slotType().validatorCheck(stack, access, player);
    }

    @Override
    public boolean managesFlight() {
        return true;
    }

    @Override
    public boolean managesEnchantmentEffects() {
        return true;
    }

    @Override
    public boolean managesXpRepair() {
        return true;
    }

    /**
     * The Trinkets fork ships exactly one wings render asset, targeting
     * {@code minecraft:elytra} and {@code #c:elytra}. Any other glider would be
     * worn invisibly, so this mod draws those itself.
     */
    @Override
    public boolean managesRenderingOf(ItemStack stack) {
        return stack.is(Items.ELYTRA) || stack.is(COMMON_ELYTRA_TAG);
    }

    @Override
    public boolean managesUseEquip() {
        return true;
    }

    @Override
    public boolean managesDeathDrops() {
        return true;
    }

    @Nullable
    private static TrinketSlotAccess access(Player player) {
        TrinketSlotAccess access = TrinketsApi.getAttachment(player).getSlotAccess(SLOT_ID, 0);
        return access != null && access.isValid() ? access : null;
    }
}
