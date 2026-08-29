package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.host.ElytraArmorSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends the elytra slot to the inventory menu on both sides, unconditionally —
 * visibility is dynamic via Slot.isActive, so server and client menus always
 * agree structurally regardless of config or host mode (this fixes a
 * client/server slot-count desync class from the old design).
 *
 * <p>Priority 500 runs this before other menu-extending mods' RETURN injections
 * so the slot usually lands at vanilla index 46 on both sides; all lookup code
 * still resolves the slot by class identity, never by index.
 */
@Mixin(value = InventoryMenu.class, priority = 500)
public abstract class InventoryMenuSlotMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void elytraslot$addElytraSlot(Inventory inventory, boolean active, Player player,
                                          CallbackInfo ci) {
        InventoryMenu self = (InventoryMenu) (Object) this;
        self.addSlot(new ElytraArmorSlot(inventory, player, -25, 8));
    }
}
