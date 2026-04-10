package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void elytraslot$addElytraSlot(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        var container = ((IElytraSlotPlayer) player).elytraslot_getElytraContainer();
        menu.addSlot(new Slot(container, 0, -25, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!ElytraSlotUtil.isElytraLike(stack)) return false;
                ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
                return !ElytraSlotUtil.isElytraLike(chestItem);
            }

            @Override
            public Identifier getNoItemIcon() {
                return Identifier.fromNamespaceAndPath("elytraslot", "container/slot/elytra");
            }
        });
    }
}
