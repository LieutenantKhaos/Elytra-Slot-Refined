package com.warwa.elytraslot.client;

import com.warwa.elytraslot.host.ElytraArmorSlot;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Gives the elytra slot a forgiving surround, whichever mod provides it.
 *
 * <p>Vanilla's armour slots sit inside the GUI, so missing one with an item on the
 * cursor just hits the background and nothing happens. An elytra slot drawn outside
 * the GUI's rectangle has no such cushion: a near-miss counts as clicking "outside"
 * and throws the item on the ground. This treats the area immediately around the
 * slot as inside, so a near-miss does nothing at all.
 */
public final class ElytraSlotClickGuard {

    /**
     * How far outside the slot still counts as a near-miss. Matches the inset of our
     * own panel, so its whole frame is covered.
     */
    private static final int MARGIN = ElytraPanelLayout.SLOT_INSET;

    /** Vanilla's slot hit box is the 16x16 item area grown by one pixel each way. */
    private static final int SLOT_BOX = 18;

    private ElytraSlotClickGuard() {
    }

    /**
     * True when the cursor is on or just beside an elytra slot in this menu, and a
     * click there should therefore not be treated as a click outside the GUI.
     */
    public static boolean nearElytraSlot(AbstractContainerMenu menu, int guiLeft, int guiTop,
                                         double mouseX, double mouseY) {
        ElytraHost host = ElytraHosts.client();
        for (Slot slot : menu.slots) {
            boolean ours = slot instanceof ElytraArmorSlot elytraSlot && elytraSlot.isActive();
            if (!ours && !host.isHostSlot(slot)) {
                continue;
            }
            int left = guiLeft + slot.x - 1 - MARGIN;
            int top = guiTop + slot.y - 1 - MARGIN;
            if (mouseX >= left && mouseX < left + SLOT_BOX + 2 * MARGIN
                && mouseY >= top && mouseY < top + SLOT_BOX + 2 * MARGIN) {
                return true;
            }
        }
        return false;
    }
}
