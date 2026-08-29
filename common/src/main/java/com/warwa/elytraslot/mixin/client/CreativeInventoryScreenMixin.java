package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.host.ElytraArmorSlot;
import com.warwa.elytraslot.host.PanelSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative INVENTORY tab support. Vanilla wraps inventory-menu slots into the
 * tab with SlotWrapper; some mods hard-cap that wrap loop at 46 slots, so any
 * wrapper vanilla may have created for the elytra slot is removed and a fresh
 * one is appended at (127, 20) — mirroring the shield slot on the opposite side
 * of the armor column. The 18x18 shield-style frame is blitted manually because
 * creative slot frames are baked into the tab background texture.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin
    extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Unique
    private static final Identifier ELYTRASLOT$TAB_INVENTORY_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_inventory.png");

    @Shadow
    private static CreativeModeTab selectedTab;

    private CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                         Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void elytraslot$wrapElytraSlot(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ElytraArmorSlot elytraSlot = PanelSlots.find(player.inventoryMenu);
        if (elytraSlot == null || !elytraSlot.isActive()) {
            return;
        }
        // Drop the wrapper vanilla's loop added for the elytra slot, then add ours.
        // SlotWrapper passes the wrapped slot's MENU index as its container slot, so
        // that — not the BODY container index — is what identifies it here.
        this.menu.slots.removeIf(slot -> slot.getContainerSlot() == elytraSlot.index);
        this.menu.slots.add(new CreativeModeInventoryScreen.SlotWrapper(
            elytraSlot, elytraSlot.index, 127, 20));
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void elytraslot$drawSlotFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          float partialTick, CallbackInfo ci) {
        if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ElytraArmorSlot elytraSlot = PanelSlots.find(player.inventoryMenu);
        if (elytraSlot == null || !elytraSlot.isActive()) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, ELYTRASLOT$TAB_INVENTORY_TEXTURE,
            this.leftPos + 126, this.topPos + 19, 34.0F, 19.0F, 18, 18, 256, 256);
    }
}
