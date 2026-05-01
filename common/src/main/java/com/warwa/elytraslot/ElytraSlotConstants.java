package com.warwa.elytraslot;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ElytraSlotConstants {
    public static final String MOD_ID = "elytraslot";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Index our elytra slot lands at on a clean vanilla install (vanilla menu has
     * slots 0..45; we add one more at the end).
     *
     * <p><b>Do not use this for runtime slot lookups.</b> Other mods (Trinkets, Curios,
     * Accessories, etc.) may also add slots in {@code InventoryMenu.<init>} at
     * {@code @Inject(at=RETURN)}; whichever mixin's {@code addSlot} call runs first
     * gets index 46. Mixin execution order is not stable across mod packs.
     *
     * <p>For runtime slot lookup, match by container identity instead — see
     * {@code InventoryMenuQuickMoveMixin#findElytraSlotIndex} or check
     * {@code slot.container instanceof ElytraSlotContainer} directly. There's only
     * one {@code ElytraSlotContainer} per player, so identity matching is unambiguous.
     */
    public static final int ELYTRA_SLOT_INDEX = 46;
}
