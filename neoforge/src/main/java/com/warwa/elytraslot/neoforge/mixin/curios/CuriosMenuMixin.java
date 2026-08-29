package com.warwa.elytraslot.neoforge.mixin.curios;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.neoforge.compat.curios.CuriosHost;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.container.CuriosMenu;

/**
 * Lets a glider be shift-clicked out of the Curios slot.
 *
 * <p>Curios' own quick-move sends anything whose equipment slot is humanoid armor to
 * the vanilla armor slot first. For a glider leaving our Curios slot that chest slot
 * is refused — correctly, since at that instant the glider is still in the Curios
 * slot and our exclusivity veto sees it — and Curios then abandons the entire click
 * rather than falling through, so the item cannot be removed by shift-click at all.
 *
 * <p>Reporting a non-armor slot for exactly that case skips the armor branch, and
 * Curios' own fallback moves the stack into the inventory like any other curio.
 */
@Mixin(value = CuriosMenu.class, remap = false)
public abstract class CuriosMenuMixin {

    @ModifyExpressionValue(
        method = "quickMoveStack",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getEquipmentSlotForItem(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/EquipmentSlot;"))
    private EquipmentSlot elytraslot$dontRouteGliderToChest(EquipmentSlot original,
                                                            Player player, int index) {
        if (original != EquipmentSlot.CHEST) {
            return original;
        }
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= self.slots.size()) {
            return original;
        }
        Slot source = self.slots.get(index);
        ItemStack stack = source.getItem();
        if (!Gliders.isGlider(stack)) {
            return original;
        }
        // Leaving a Curios slot: skip the armor branch so Curios' fallback moves it
        // into the inventory instead of abandoning the click.
        if (source instanceof CurioSlot) {
            return EquipmentSlot.MAINHAND;
        }
        // Entering from the inventory: skip the armor branch so Curios' own
        // move-into-curio-slots branch runs, instead of equipping to the chest.
        if (CuriosHost.INSTANCE.get(player).isEmpty()
            && CuriosHost.INSTANCE.canAccept(player, stack)) {
            return EquipmentSlot.MAINHAND;
        }
        return original;
    }
}
