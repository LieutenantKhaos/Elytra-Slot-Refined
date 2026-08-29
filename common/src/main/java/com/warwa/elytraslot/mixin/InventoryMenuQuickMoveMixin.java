package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraArmorSlot;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import com.warwa.elytraslot.host.PanelSlots;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click handling for the elytra slot. Three cases need interception; every
 * other click falls through to vanilla untouched.
 *
 * <ul>
 *   <li><b>In</b>: a glider in the inventory or hotbar goes to the elytra slot,
 *       mirroring how vanilla prefers armor slots for armor.</li>
 *   <li><b>Out</b>: a glider leaving the elytra slot goes to the inventory.
 *       Vanilla would route it to the chest armor slot, which the exclusivity
 *       veto refuses, and vanilla then abandons the whole click.</li>
 *   <li><b>Spare glider</b>: when the elytra slot cannot take it and the chest
 *       slot is empty, vanilla again tries the chest, gets vetoed, and dead-ends;
 *       this performs vanilla's normal inventory/hotbar shuffle instead.</li>
 * </ul>
 *
 * <p>Extends AbstractContainerMenu only to reach the protected moveItemStackTo;
 * the constructor is never called.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuQuickMoveMixin extends AbstractContainerMenu {

    @Unique
    private static final int INVENTORY_START = 9;
    @Unique
    private static final int HOTBAR_START = 36;
    @Unique
    private static final int HOTBAR_END = 45;
    @Unique
    private static final int OFFHAND = 45;

    private InventoryMenuQuickMoveMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void elytraslot$quickMove(Player player, int index,
                                      CallbackInfoReturnable<ItemStack> cir) {
        ElytraArmorSlot elytraSlot = PanelSlots.find(this);
        boolean panelUsable = elytraSlot != null && elytraSlot.isActive();

        if (panelUsable && index == elytraSlot.index) {
            ItemStack source = elytraSlot.getItem();
            if (source.isEmpty()) {
                return;
            }
            ItemStack before = source.copy();
            if (!this.moveItemStackTo(source, INVENTORY_START, HOTBAR_END, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            elytraslot$finish(elytraSlot, player, source, before, cir);
            return;
        }

        if (index < INVENTORY_START || index > OFFHAND) {
            return; // inventory (9..35), hotbar (36..44) and the offhand (45)
        }
        Slot sourceSlot = this.slots.get(index);
        ItemStack source = sourceSlot.getItem();
        if (!Gliders.isChestGlider(source)) {
            return;
        }

        ItemStack before = source.copy();
        ElytraHost host = ElytraHosts.forSide(player);
        boolean moved;
        if (panelUsable && !elytraSlot.hasItem() && elytraSlot.mayPlace(source)) {
            moved = this.moveItemStackTo(source, elytraSlot.index, elytraSlot.index + 1, false);
        } else if (!host.isBuiltin() && host.get(player).isEmpty()
            && host.canAccept(player, source)) {
            // The accessory mod's slot is not part of this menu, so moveItemStackTo
            // cannot reach it — write it directly and shrink the source by hand.
            // Without this, vanilla routes the glider to the chest armour slot.
            ItemStack single = source.split(1);
            moved = host.set(player, single, false);
            if (!moved) {
                source.grow(single.getCount()); // a refused write must not destroy it
            }
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            && !player.isEquippableInSlot(source, EquipmentSlot.CHEST)) {
            // Vanilla would try the empty chest slot, our exclusivity veto refuses,
            // and vanilla abandons the click. Do its normal shuffle instead.
            if (index == OFFHAND) {
                moved = this.moveItemStackTo(source, INVENTORY_START, HOTBAR_END, false);
            } else if (index < HOTBAR_START) {
                moved = this.moveItemStackTo(source, HOTBAR_START, HOTBAR_END, false);
            } else {
                moved = this.moveItemStackTo(source, INVENTORY_START, HOTBAR_START, false);
            }
        } else {
            return; // vanilla handles this correctly on its own
        }
        if (!moved) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        elytraslot$finish(sourceSlot, player, source, before, cir);
    }

    /** Vanilla's quickMoveStack tail: slot bookkeeping and the return protocol. */
    @Unique
    private static void elytraslot$finish(Slot slot, Player player, ItemStack source,
                                          ItemStack before, CallbackInfoReturnable<ItemStack> cir) {
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, before);
        } else {
            slot.setChanged();
        }
        if (source.getCount() == before.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, source);
        cir.setReturnValue(before);
    }
}
