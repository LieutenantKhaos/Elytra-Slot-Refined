package com.warwa.elytraslot.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Left-side elytra-slot panel drawn as a 9-slice from the vanilla inventory texture.
 *
 * Every pixel in the panel chrome (corners, edges, bevels) is sampled 1:1 from
 * {@code minecraft:textures/gui/container/inventory.png}, so the look is pixel-for-pixel
 * identical to the Minecraft inventory panel itself. The panel interior is filled with the
 * vanilla body-gray ARGB (0xFFC6C6C6) because the inventory texture has no large plain-gray
 * patch to sample from (its interior is full of slots / crafting grid / black portrait area).
 *
 * Geometry:
 *   - Panel: 32 x 32 square at (leftPos - 33, topPos). Rightmost panel pixel sits at
 *     leftPos - 2, leaving column (leftPos - 1) as a 1-pixel gap before the inventory's own
 *     border, which starts at leftPos.
 *   - Slot: 18 x 18 at (leftPos - 26, topPos + 7) - perfectly centered in the panel with
 *     exactly 7 px padding on all four sides.
 *   - Slot item lands at (leftPos - 25, topPos + 8), matching the
 *     {@code Slot(container, 0, -25, 8)} object added by
 *     {@link com.warwa.elytraslot.mixin.InventoryMenuMixin}. y+8 also matches the helmet armor
 *     slot's item-Y, keeping the elytra slot aligned with the inventory's top row of slots.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenBgMixin extends AbstractContainerScreen<InventoryMenu> {

    private InventoryScreenBgMixin() { super(null, null, null); }

    private static final Identifier INVENTORY_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");

    // Vanilla inventory panel dimensions inside the 256x256 atlas.
    private static final int INV_W  = 176;
    private static final int INV_H  = 166;
    private static final int BORDER = 3;

    // Vanilla inventory body-gray, used to fill the panel interior.
    private static final int PANEL_BODY = 0xFFC6C6C6;

    /**
     * Extend the click boundary to include the elytra panel to the left.
     * Without this, clicks on the elytra slot are treated as "outside" the inventory
     * and the item gets dropped instead of picked up.
     */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (mx >= xo - 33 && mx < xo && my >= yo && my < yo + 32) {
            return false;
        }
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void elytraslot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int x = this.leftPos;
        int y = this.topPos;

        int panelW = 32;
        int panelH = 32;
        int panelX = x - panelW - 1;   // 1-px gap: rightmost panel pixel at x-2, inventory at x
        int panelY = y;

        int innerW = panelW - 2 * BORDER;   // 26
        int innerH = panelH - 2 * BORDER;   // 26
        int rX = panelX + panelW - BORDER;
        int bY = panelY + panelH - BORDER;
        int srcR = INV_W - BORDER;
        int srcB = INV_H - BORDER;

        // 4 corners - 3x3 blits from the four corners of the vanilla inventory panel.
        // These carry the pixel-exact vanilla rounded-corner art.
        blit(graphics, panelX, panelY, 0,    0,    BORDER, BORDER);   // TL
        blit(graphics, rX,     panelY, srcR, 0,    BORDER, BORDER);   // TR
        blit(graphics, panelX, bY,     0,    srcB, BORDER, BORDER);   // BL
        blit(graphics, rX,     bY,     srcR, srcB, BORDER, BORDER);   // BR

        // 4 edges - 1:1 blits from the vanilla inventory's border strips.
        // Same 3-px thickness on every side.
        blit(graphics, panelX + BORDER, panelY,          BORDER, 0,      innerW, BORDER);   // top
        blit(graphics, panelX + BORDER, bY,              BORDER, srcB,   innerW, BORDER);   // bottom
        blit(graphics, panelX,          panelY + BORDER, 0,      BORDER, BORDER, innerH);   // left
        blit(graphics, rX,              panelY + BORDER, srcR,   BORDER, BORDER, innerH);   // right

        // Center - solid vanilla body-gray. (Can't blit a plain-gray patch out of the
        // inventory texture at this size since the interior is full of slots and the black
        // player-portrait area.)
        graphics.fill(panelX + BORDER, panelY + BORDER, panelX + panelW - BORDER, panelY + panelH - BORDER, PANEL_BODY);

        // Corner bevel "bulge" pixels that the 3x3 corner blits miss — vanilla's panel has
        // one extra white-highlight pixel one step inside the TL corner, and one extra
        // dark-gray shadow pixel one step inside the BR corner, both of which would be
        // hidden by the solid-gray center fill above. Blit them 1 px at a time.
        blit(graphics, panelX + BORDER,                 panelY + BORDER,                 BORDER,          BORDER,          1, 1); // TL bulge — WW at vanilla (3,3)
        blit(graphics, panelX + panelW - BORDER - 1,    panelY + panelH - BORDER - 1,    srcR - 1,        srcB - 1,        1, 1); // BR bulge — 85 at vanilla (srcR-1, srcB-1) = (172, 162)

        // Slot recess - 1:1 blit of the 18 x 18 vanilla helmet-slot region at UV (7, 7).
        int slotX = x - 26;
        int slotY = y + 7;
        blit(graphics, slotX, slotY, 7, 7, 18, 18);
    }

    private static void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x, y, (float) u, (float) v, w, h, 256, 256);
    }
}
