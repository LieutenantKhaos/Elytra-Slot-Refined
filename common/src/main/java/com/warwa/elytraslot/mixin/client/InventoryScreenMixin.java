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
 * Positions the Elytra Slot in the survival inventory and draws a vanilla
 * 18x18 slot recess behind it.
 *
 * <p>No 32x32 side panel is drawn. Creative inventory behavior is untouched.
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
    private void elytraslot$drawSlotBackground(GuiGraphicsExtractor graphics,
                                                int mouseX,
                                                int mouseY,
                                                float partialTick,
                                                CallbackInfo ci) {
        ElytraArmorSlot slot = PanelSlots.find(this.menu);
        if (slot == null || !slot.isActive()) {
            return;
        }

        int slotX = ElytraPanelLayout.slotX();
        int slotY = ElytraPanelLayout.slotY();

        // Move the actual interactive slot.
        slot.elytraslot$moveTo(slotX, slotY);

        // Draw the same 18x18 recessed background used by vanilla armor slots.
        // Slot coordinates describe the 16x16 item area, so the recess begins
        // one pixel above and to the left.
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ELYTRASLOT$INVENTORY_TEXTURE,
            this.leftPos + slotX - 1,
            this.topPos + slotY - 1,
            7,
            7,
            18,
            18,
            256,
            256
        );
    }
}
