package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two appearance fixes for the elytra slot on the creative INVENTORY tab:
 *
 * <ol>
 *   <li><b>Slot wrapper.</b> Vanilla's {@code selectTab} loops over every slot in the
 *       player's {@link InventoryMenu}, wraps each one in a {@code SlotWrapper} at a
 *       layout-appropriate position, and adds it to the {@code ItemPickerMenu.slots}
 *       list. The vanilla generic-formula position for our slot index (46+) collides
 *       with hotbar slot 1, and we want it at (127, 20) — mirror of the shield slot.
 *
 *       <p>We can't safely rely on the vanilla loop processing our slot because
 *       <b>Trinkets Updated installs a {@code @Redirect} on the loop's
 *       {@code NonNullList.size()} call that hard-codes the return to 46</b>. That
 *       silently truncates the iteration so any slot at index 46+ never gets a
 *       wrapper, and our slot becomes invisible whenever Trinkets is installed
 *       (visible report: blank gray square at (127, 20) — the frame-blit fires but
 *       no slot lives there).
 *
 *       <p>Solution: a TAIL inject on {@code selectTab} that finds our slot by
 *       container identity ({@link ElytraSlotContainer} is unique per player), removes
 *       any wrapper that vanilla's loop already added for it (the no-Trinkets case),
 *       and appends a fresh {@code SlotWrapper} at (127, 20). This works in both
 *       Trinkets-present and Trinkets-absent setups without depending on Trinkets's
 *       API or coordinating with their mixin order. The construction needs the
 *       package-private {@code SlotWrapper} ctor accessible — see the access
 *       widener / transformer.</li>
 *
 *   <li><b>Slot frame.</b> Slot frames in the creative inventory are baked into the
 *       tab background texture {@code creative_inventory/tab_inventory.png}, but only
 *       at the original vanilla positions. The new (127, 20) position has no frame,
 *       so we copy the shield's 18×18 frame from source UV (34, 19) to screen
 *       position {@code (leftPos + 126, topPos + 19)} after vanilla draws the
 *       background. The frame is sourced from the same texture vanilla used, so it's
 *       pixel-identical.</li>
 * </ol>
 *
 * The third reported fix (purple-tinted empty-slot icon) is not in this class — it's
 * a texture-only change applied directly to the PNG assets.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin
    extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    private CreativeInventoryScreenMixin() { super(null, null, null); }

    @Shadow private static CreativeModeTab selectedTab;

    private static final Identifier TAB_INVENTORY_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_inventory.png");

    // Shield slot frame source in tab_inventory.png: 18×18 starting one pixel up-and-left
    // of the shield item position (35, 20).
    private static final int SHIELD_FRAME_U = 34;
    private static final int SHIELD_FRAME_V = 19;
    private static final int FRAME_SIZE     = 18;

    // Elytra slot screen position. Item top-left at (127, 20), so the 18×18 frame goes
    // to (126, 19) — mirroring the shield's frame at (34, 19) on the opposite side of
    // the armor column.
    private static final int ELYTRA_SLOT_X   = 127;
    private static final int ELYTRA_SLOT_Y   = 20;
    private static final int ELYTRA_FRAME_X  = 126;
    private static final int ELYTRA_FRAME_Y  = 19;

    /**
     * Fix #1 — ensure exactly one {@code SlotWrapper} for our elytra slot exists in the
     * picker menu, positioned at (127, 20). Runs at TAIL of {@code selectTab} so all of
     * vanilla's (and any other mod's) slot-add work has already happened by the time we
     * make our adjustments.
     */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void elytraslot$placeElytraSlotInCreativeInventory(CreativeModeTab tab, CallbackInfo ci) {
        if (tab.getType() != CreativeModeTab.Type.INVENTORY) return;

        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        InventoryMenu inventoryMenu = player.inventoryMenu;

        // Find our slot in the player's inventoryMenu by container identity. Mod-order
        // independent: the ElytraSlotContainer instance is unique per player, ours.
        Slot ourSlot = null;
        for (Slot s : inventoryMenu.slots) {
            if (s.container instanceof ElytraSlotContainer) {
                ourSlot = s;
                break;
            }
        }
        if (ourSlot == null) {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] selectTab(INVENTORY): no ElytraSlotContainer found in player.inventoryMenu — skipping"
            );
            return;
        }

        CreativeModeInventoryScreen.ItemPickerMenu pickerMenu =
            (CreativeModeInventoryScreen.ItemPickerMenu) this.menu;

        // In the no-Trinkets case, vanilla's loop already added a wrapper for our slot
        // at the generic-formula position (overlapping hotbar slot 1). Drop it so we
        // don't end up with two wrappers for the same container.
        pickerMenu.slots.removeIf(slot -> slot.container instanceof ElytraSlotContainer);

        // Construct a wrapper at the desired position. The 2nd arg mirrors what vanilla's
        // loop would have passed (the slot's index in inventoryMenu) — see vanilla
        // selectTab bytecode: `new SlotWrapper(target, i, slotX, slotY)` where
        // `target == inventoryMenu.slots.get(i)`, so the 2nd arg equals `target.index`.
        // SlotWrapper's ctor visibility is widened via accesswidener / accesstransformer.
        Slot wrapper = new CreativeModeInventoryScreen.SlotWrapper(
            ourSlot, ourSlot.index, ELYTRA_SLOT_X, ELYTRA_SLOT_Y);
        pickerMenu.slots.add(wrapper);
    }

    /**
     * Fix #2 — paint a shield-style slot frame at the elytra position. Only fires on
     * the INVENTORY tab; other tabs use entirely different layouts and don't show
     * the elytra slot.
     */
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void elytraslot$drawElytraSlotFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TAB_INVENTORY_TEXTURE,
            this.leftPos + ELYTRA_FRAME_X,
            this.topPos  + ELYTRA_FRAME_Y,
            (float) SHIELD_FRAME_U,
            (float) SHIELD_FRAME_V,
            FRAME_SIZE, FRAME_SIZE,
            256, 256
        );
    }
}
