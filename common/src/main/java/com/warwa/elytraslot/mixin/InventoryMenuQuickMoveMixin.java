package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuQuickMoveMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void elytraslot$quickMoveToElytraSlot(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        Slot sourceSlot = menu.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return;

        ItemStack stack = sourceSlot.getItem();

        // Moving OUT of elytra slot -> player inventory
        if (sourceSlot.getNoItemIcon() != null && "elytraslot".equals(sourceSlot.getNoItemIcon().getNamespace())) {
            ItemStack copy = stack.copy();
            for (int i = 9; i < 45; i++) {
                Slot target = menu.slots.get(i);
                if (!target.hasItem()) {
                    target.setByPlayer(stack.copy());
                    sourceSlot.set(ItemStack.EMPTY);
                    sourceSlot.setChanged();
                    cir.setReturnValue(copy);
                    return;
                }
            }
            return;
        }

        // Moving INTO elytra slot from inventory
        if (!ElytraSlotUtil.isElytraLike(stack)) return;

        for (Slot s : menu.slots) {
            if (s.getNoItemIcon() != null && "elytraslot".equals(s.getNoItemIcon().getNamespace())) {
                if (s.hasItem()) return;
                if (ElytraSlotUtil.isElytraLike(player.getItemBySlot(EquipmentSlot.CHEST))) return;

                ItemStack copy = stack.copy();
                s.setByPlayer(stack.copy());
                sourceSlot.set(ItemStack.EMPTY);
                sourceSlot.setChanged();
                cir.setReturnValue(copy);
                return;
            }
        }
    }
}
