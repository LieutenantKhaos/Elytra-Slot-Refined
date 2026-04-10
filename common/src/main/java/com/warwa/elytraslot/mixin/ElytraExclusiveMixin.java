package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public abstract class ElytraExclusiveMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void elytraslot$blockDoubleElytra(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ElytraSlotUtil.isElytraLike(stack)) return;

        Slot self = (Slot) (Object) this;
        if (self.getContainerSlot() != 38) return;
        if (!(self.container instanceof net.minecraft.world.entity.player.Inventory inv)) return;

        Player player = inv.player;
        ItemStack slotElytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (ElytraSlotUtil.isElytraLike(slotElytra)) {
            cir.setReturnValue(false);
        }
    }
}
