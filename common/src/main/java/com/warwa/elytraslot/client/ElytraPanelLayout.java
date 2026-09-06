package com.warwa.elytraslot.client;

/**
 * Survival-inventory position for the Elytra Slot.
 *
 * <p>The slot is placed in the same column as the vanilla offhand/shield slot
 * and on the same row as the vanilla chestplate slot.
 */
public final class ElytraPanelLayout {

    /**
     * Retained for ElytraSlotClickGuard compatibility.
     */
    public static final int SLOT_INSET = 8;

    /**
     * Vanilla inventory-menu coordinates.
     *
     * <p>x=77 is the offhand/shield column.
     * y=26 is the chestplate row.
     */
    private static final int SURVIVAL_SLOT_X = 77;
    private static final int SURVIVAL_SLOT_Y = 26;

    private ElytraPanelLayout() {
    }

    public static int slotX() {
        return SURVIVAL_SLOT_X;
    }

    public static int slotY() {
        return SURVIVAL_SLOT_Y;
    }
}
