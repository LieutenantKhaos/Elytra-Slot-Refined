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

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenBgMixin extends AbstractContainerScreen<InventoryMenu> {

    private InventoryScreenBgMixin() { super(null, null, null); }

    // Bundled copy of the curios inventory panel texture
    private static final Identifier CURIOS_INVENTORY_TEXTURE =
            Identifier.fromNamespaceAndPath("elytraslot", "textures/gui/inventory_panel.png");

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void elytraslot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int x = this.leftPos;
        int y = this.topPos;

        // Exact vanilla CuriosScreen bytecode approach - 5 blits for single elytra slot
        // Same UV regions, positions, and rendering as curios panel with slotCount=1
        int bodyH = 7 + 1 * 18;  // 25px: 7px header + 1 slot row
        int panelX = x - 33;     // bipush -33 from CuriosScreen bytecode

        // 1. Left body: UV(91,0) W=25 H=25
        graphics.blit(RenderPipelines.GUI_TEXTURED, CURIOS_INVENTORY_TEXTURE,
                panelX, y,
                91.0F, 0.0F, 25, bodyH, 256, 256);

        // 2. Left bottom: UV(91,159) W=25 H=7
        graphics.blit(RenderPipelines.GUI_TEXTURED, CURIOS_INVENTORY_TEXTURE,
                panelX, y + bodyH,
                91.0F, 159.0F, 25, 7, 256, 256);

        // 3. Right body: UV(98,0) W=25 H=25 at panelX+7
        graphics.blit(RenderPipelines.GUI_TEXTURED, CURIOS_INVENTORY_TEXTURE,
                panelX + 7, y,
                98.0F, 0.0F, 25, bodyH, 256, 256);

        // 4. Right bottom: UV(98,159) W=25 H=7 at panelX+7
        graphics.blit(RenderPipelines.GUI_TEXTURED, CURIOS_INVENTORY_TEXTURE,
                panelX + 7, y + bodyH,
                98.0F, 159.0F, 25, 7, 256, 256);

        // 5. Slot background: UV(7,7) W=18 H=18
        graphics.blit(RenderPipelines.GUI_TEXTURED, CURIOS_INVENTORY_TEXTURE,
                x - 26, y + 7,
                7.0F, 7.0F, 18, 18, 256, 256);
    }
}
