package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.client.ElytraPanelLayout;
import com.warwa.elytraslot.host.ElytraArmorSlot;
import com.warwa.elytraslot.host.PanelSlots;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the elytra slot's side panel: a 32x32 nine-slice sampled 1:1 from vanilla
 * inventory.png, with an 18x18 helmet-style slot recess centred in it. Every
 * coordinate derives from {@link ElytraPanelLayout}, which also decides which side
 * of the GUI the panel is on, so the drawn panel, the click rectangle and the
 * slot's own position always agree.
 *
 * <p>The art numbers are preserved from the previous version and must not drift:
 * corner UVs (0,0)/(173,0)/(0,163)/(173,163), edge UVs (3,0)/(3,163)/(0,3)/(173,3)
 * with 26px runs, centre fill 0xFFC6C6C6, two 1x1 bulge-correction blits, and an
 * 18x18 recess blit taken from the helmet slot at UV(7,7), inset 7px in the panel.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Unique
    private static final Identifier ELYTRASLOT$INVENTORY_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");

    private InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void elytraslot$drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                      float partialTick, CallbackInfo ci) {
        ElytraArmorSlot slot = PanelSlots.find(this.menu);
        if (slot == null || !slot.isActive()) {
            return;
        }
        boolean bookOpen = ElytraPanelLayout.recipeBookOpen(this);
        // Keep the slot under the panel: the recipe book can move the panel between
        // frames, and the slot's position drives both rendering and hit-testing.
        slot.elytraslot$moveTo(ElytraPanelLayout.slotX(this.imageWidth, bookOpen),
            ElytraPanelLayout.slotY());

        int panelX = ElytraPanelLayout.panelLeft(this.leftPos, this.imageWidth, bookOpen);
        int panelY = this.topPos;

        // Corners (3x3).
        elytraslot$blit(graphics, panelX, panelY, 0, 0, 3, 3);
        elytraslot$blit(graphics, panelX + 29, panelY, 173, 0, 3, 3);
        elytraslot$blit(graphics, panelX, panelY + 29, 0, 163, 3, 3);
        elytraslot$blit(graphics, panelX + 29, panelY + 29, 173, 163, 3, 3);
        // Edges (3px thick, 26px runs).
        elytraslot$blit(graphics, panelX + 3, panelY, 3, 0, 26, 3);
        elytraslot$blit(graphics, panelX + 3, panelY + 29, 3, 163, 26, 3);
        elytraslot$blit(graphics, panelX, panelY + 3, 0, 3, 3, 26);
        elytraslot$blit(graphics, panelX + 29, panelY + 3, 173, 3, 3, 26);
        // Interior fill (inventory.png has no plain gray patch this size).
        graphics.fill(panelX + 3, panelY + 3, panelX + 29, panelY + 29, 0xFFC6C6C6);
        // The two bulge pixels the corner slices miss: highlight and shadow.
        elytraslot$blit(graphics, panelX + 3, panelY + 3, 3, 3, 1, 1);
        elytraslot$blit(graphics, panelX + 28, panelY + 28, 172, 162, 1, 1);
        // The 18x18 slot recess, sampled from the helmet slot.
        elytraslot$blit(graphics, panelX + 7, panelY + 7, 7, 7, 18, 18);
    }

    @Unique
    private static void elytraslot$blit(GuiGraphicsExtractor graphics, int x, int y, float u, float v,
                                        int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ELYTRASLOT$INVENTORY_TEXTURE,
            x, y, u, v, width, height, 256, 256);
    }
}
