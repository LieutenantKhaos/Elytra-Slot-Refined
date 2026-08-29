package com.warwa.elytraslot.client;

/**
 * Where the elytra panel sits on the inventory screen.
 *
 * <p>By default it hangs off the left edge. The recipe book occupies exactly that
 * space when open — vanilla shifts the whole GUI right to make room for it, and
 * draws the book in a later stratum, which would leave our panel invisible but
 * still clickable. So while the book is open the panel moves to the right edge
 * instead, keeping the slot visible and usable either way.
 *
 * <p>Drawing, hit-testing and the slot's own position all derive from here so
 * they cannot drift apart. Callers pass the screen geometry in because those
 * fields are protected to the screen hierarchy.
 */
public final class ElytraPanelLayout {

    /** Panel edge length, and the slot's inset within it. */
    public static final int PANEL_SIZE = 32;
    public static final int SLOT_INSET = 8;

    /** Gap between the panel and the GUI edge. */
    private static final int GAP = 1;

    private ElytraPanelLayout() {
    }

    public static boolean recipeBookOpen(Object screen) {
        return screen instanceof RecipeBookAware aware && aware.elytraslot$isRecipeBookOpen();
    }

    /** Left edge of the panel, in screen coordinates. */
    public static int panelLeft(int leftPos, int imageWidth, boolean recipeBookOpen) {
        return leftPos + panelOffset(imageWidth, recipeBookOpen);
    }

    /** The slot's x in menu coordinates (relative to leftPos), matching the panel. */
    public static int slotX(int imageWidth, boolean recipeBookOpen) {
        return panelOffset(imageWidth, recipeBookOpen) + SLOT_INSET;
    }

    /** The slot's y in menu coordinates (relative to topPos). */
    public static int slotY() {
        return SLOT_INSET;
    }

    private static int panelOffset(int imageWidth, boolean recipeBookOpen) {
        return recipeBookOpen ? imageWidth + GAP : -(PANEL_SIZE + GAP);
    }
}
