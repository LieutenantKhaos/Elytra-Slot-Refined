package com.warwa.elytraslot.compat.trinkets;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHosts;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Trinkets Updated integration. All {@code eu.pb4.trinkets} references live in this
 * package and are only classloaded behind an isModLoaded gate.
 *
 * <p>The slot itself is registered data-driven ({@code data/trinkets/slots/chest/elytra.json}
 * + {@code data/trinkets/entities/elytraslot.json}); this class supplies the code half:
 * the slot's validator predicate and the right-click-equip callback for glider items.
 */
public final class TrinketsCompat {

    private TrinketsCompat() {
    }

    /** Called at loader init, before ElytraSlot.init resolves the active host. */
    public static void register() {
        ElytraHosts.registerExternal(TrinketsHost.INSTANCE);

        // Referenced by "validator_predicates": ["elytraslot:glider"] in the slot data.
        // Accepts chest-equippable gliders only while Trinkets hosts the elytra, and
        // enforces exclusivity: never while the vanilla chest slot or this mod's own
        // BODY slot already holds a glider.
        TrinketsApi.registerTrinketPredicate(ElytraSlot.id("glider"), (stack, slot, entity) ->
            ElytraHosts.forSide(entity) == TrinketsHost.INSTANCE
                && Gliders.isChestGlider(stack)
                && !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.CHEST))
                && !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.BODY)));
    }

    private static boolean callbacksRegistered;

    /**
     * Gives every glider item a Trinkets callback so right-clicking it in hand
     * equips it into the trinket slot.
     *
     * <p>Must run late: item default components are not bound during mod init, so
     * scanning the registry any earlier throws. Loaders call this when a server
     * starts and when a client joins a world, whichever comes first; the scan is
     * one-shot.
     */
    public static synchronized void registerGliderCallbacks() {
        if (callbacksRegistered) {
            return;
        }
        callbacksRegistered = true;
        if (ElytraHosts.server() != TrinketsHost.INSTANCE) {
            return; // another host owns the elytra; leave Trinkets' callbacks alone
        }
        TrinketCallback callback = new GliderTrinketCallback();
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack sample = item.getDefaultInstance();
            if (!Gliders.isChestGlider(sample)) {
                continue; // only items our slot can actually accept
            }
            // Never shadow a callback the item already has: setCallback is a plain
            // overwrite, and we run last, so this would silently drop another mod's
            // ticking, attribute modifiers and drop rules for its own winged item.
            if (item instanceof TrinketCallback
                || TrinketCallback.getCallback(sample) != TrinketCallback.DEFAULT) {
                continue;
            }
            TrinketCallback.setCallback(item, callback);
            count++;
        }
        ElytraSlot.LOGGER.debug("Registered Trinkets glider callback for {} items", count);
    }

    private static final class GliderTrinketCallback implements TrinketCallback {
        @Override
        public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
            // Route right-click equip into the trinket slot only while Trinkets hosts
            // the elytra and no glider is worn elsewhere. Returning false here lets the
            // click fall through to vanilla (or this mod's own swap handling) instead
            // of Trinkets blocking it with a FAIL result when no slot would accept —
            // which is also why the stack must match what the slot validator accepts.
            return Gliders.isChestGlider(stack)
                && ElytraHosts.forSide(entity) == TrinketsHost.INSTANCE
                && !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.CHEST))
                && !Gliders.isGlider(entity.getItemBySlot(EquipmentSlot.BODY));
        }
        // getEquipSound's default already resolves the vanilla EQUIPPABLE component's
        // equip sound (elytra sound for elytras); drop rules default to vanilla parity.
    }
}
