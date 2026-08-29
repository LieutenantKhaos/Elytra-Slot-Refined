package com.warwa.elytraslot.neoforge.compat.curios;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;
import top.theillusivec4.curios.api.internal.CuriosServices;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Curios integration (NeoForge only). All {@code top.theillusivec4.curios}
 * references live in this package and are only classloaded behind an
 * isModLoaded gate.
 *
 * <p>The slot itself is registered data-driven
 * ({@code data/elytraslot/curios/slots/elytra.json}); this class supplies the
 * code half: the slot's validator predicate and the curio behavior for gliders.
 */
public final class CuriosCompat {

    private CuriosCompat() {
    }

    /** Called from the mod constructor, before ElytraSlot.init resolves the host. */
    public static void register() {
        ElytraHosts.registerExternal(CuriosHost.INSTANCE);

        // Referenced by "validators": ["elytraslot:glider"] in the slot data.
        // Type-eligibility only; exclusivity is enforced by GliderCurio.canEquip.
        // Curios asks this with a null entity when resolving tooltips and default
        // attribute modifiers, so the stack test comes first and the wearer-less
        // query answers from the locally resolved host.
        CuriosSlotTypes.registerPredicate(ElytraSlot.id("glider"), (slotContext, stack) ->
            Gliders.isChestGlider(stack)
                && ElytraHosts.forSide(slotContext.entity()) == CuriosHost.INSTANCE);
    }

    private static boolean curiosRegistered;

    /**
     * Gives every glider item — modded elytras included — curio behavior:
     * right-click equip, exclusivity, and the vanilla equip sound.
     *
     * <p>Must run late: item default components are not bound during mod loading,
     * so scanning the registry any earlier throws. Called when a server starts and
     * when a client joins a world, whichever comes first; the scan is one-shot.
     */
    public static synchronized void registerGliderCurios() {
        if (curiosRegistered) {
            return;
        }
        curiosRegistered = true;
        // Not gated on the resolved host: a client may resolve differently from the
        // server it joins, and the behavior has to exist on both sides for
        // right-click equip to agree. GliderCurio itself refuses every slot unless
        // Curios is the active host, so registering it is inert in other modes.
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!Gliders.isChestGlider(item.getDefaultInstance())) {
                continue; // only items our slot can actually accept
            }
            // Never shadow a curio behavior the item already has: registering here
            // wins over both the registry and ICurioItem-implementing items, which
            // would silently drop another mod's tick/equip/drop handling.
            if (item instanceof ICurioItem || CuriosServices.EXTENSIONS.getCurioItem(item) != null) {
                continue;
            }
            CuriosApi.registerCurio(item, GliderCurio.INSTANCE);
            count++;
        }
        ElytraSlot.LOGGER.debug("Registered curio behavior for {} glider items", count);
    }
}
