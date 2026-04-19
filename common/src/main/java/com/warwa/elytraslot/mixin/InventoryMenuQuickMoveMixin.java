package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraEquipEffects;
import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotContainer;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click routing for our custom elytra slot. Injects at HEAD of
 * {@link InventoryMenu#quickMoveStack(Player, int)} and handles THREE cases by
 * delegating to vanilla's {@link AbstractContainerMenu#moveItemStackTo(ItemStack, int, int, boolean)}:
 *
 * <ol>
 *   <li><b>Source = custom elytra slot</b>: shift-click from our slot to inventory/hotbar.
 *       Uses vanilla slot-search order: main inventory first (9..36), then hotbar (36..45).
 *       moveItemStackTo handles both stack-merging (pass 1, doesn't check mayPlace) and
 *       empty-slot fill (pass 2, does check mayPlace). UI3 + UI4 fixes.</li>
 *   <li><b>Source = inventory/hotbar, stack is elytra-like, slot empty, chest empty</b>:
 *       shift-click INTO our slot. Uses moveItemStackTo with our slot's index range
 *       so mayPlace is called server-side (SE2 fix) and stack merging works (UI5).</li>
 *   <li>Otherwise: no interception — vanilla handles the move.</li>
 * </ol>
 *
 * <p>For case (1), we also call {@link ElytraEquipEffects#onSlotChanged} after a
 * successful move out so unequip side-effects (attribute modifier remove,
 * enchantment stop-effects, game-event UNEQUIP) fire even when the player
 * shift-clicks the elytra out of the slot instead of right-clicking it off.
 *
 * <p>The custom elytra slot's index is not hard-coded — we search for it by
 * container identity ({@link ElytraSlotContainer}) to tolerate other mods that
 * add slots after ours.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuQuickMoveMixin extends AbstractContainerMenu {

    protected InventoryMenuQuickMoveMixin() { super(null, 0); } // never called; mixin compat

    /** Standard vanilla slot ranges (from InventoryMenu constants). */
    private static final int INV_START = 9;
    private static final int INV_END_EXCLUSIVE = 36;
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END_EXCLUSIVE = 45;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void elytraslot$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        InventoryMenu menu = (InventoryMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) return;

        Slot sourceSlot = menu.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return;

        int elytraIdx = findElytraSlotIndex(menu, player);
        if (elytraIdx < 0) {
            // Custom slot not present (shouldn't happen). Let vanilla handle.
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] quickMove custom slot not found for player={} — falling through to vanilla",
                player.getName().getString()
            );
            return;
        }

        if (index == elytraIdx) {
            // ─── Case 1: Shift-click OUT of the elytra slot ────────────────────────────
            ItemStack beforeMove = sourceSlot.getItem();
            ItemStack preMoveSnapshot = beforeMove.copy();
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] quickMove OUT beforeMove={} player={}",
                beforeMove, player.getName().getString()
            );

            // Vanilla slot-search order for moving out of an armor slot (from
            // InventoryMenu.quickMoveStack at index 5-8): moveItemStackTo(stack, 9, 45, false)
            // — main inventory first, then hotbar, non-reversed.
            boolean moved = this.moveItemStackTo(beforeMove, INV_START, HOTBAR_END_EXCLUSIVE, false);
            if (!moved) {
                ElytraSlotConstants.LOGGER.info(
                    "[elytraslot] quickMove OUT no destination available — return EMPTY"
                );
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }

            if (beforeMove.isEmpty()) {
                sourceSlot.setByPlayer(ItemStack.EMPTY, preMoveSnapshot);
            } else {
                sourceSlot.setChanged();
            }
            sourceSlot.onTake(player, beforeMove);

            // Slot.setByPlayer on ElytraSlotContainer will already fire onSlotChanged via
            // the container's setItem override, but moveItemStackTo's pass-2 does NOT go
            // through setByPlayer on the SOURCE — it mutates the stack directly and then
            // calls setChanged(). So the equip-effect dispatch won't fire for the SOURCE.
            // We must fire it manually to get parity with removing from a vanilla slot.
            ElytraEquipEffects.onSlotChanged(player, preMoveSnapshot, sourceSlot.getItem());

            cir.setReturnValue(preMoveSnapshot);
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] quickMove OUT SUCCESS pre={} remainingInSource={}",
                preMoveSnapshot, sourceSlot.getItem()
            );
            return;
        }

        // ─── Case 2: Shift-click INTO the elytra slot ─────────────────────────────────
        ItemStack stackInSource = sourceSlot.getItem();
        if (!ElytraSlotUtil.isElytraLike(stackInSource)) return;

        Slot elytraSlot = menu.slots.get(elytraIdx);

        // SE2 fix: even though moveItemStackTo checks mayPlace, we also test it explicitly
        // so the debug log records the decision and we fall through to vanilla cleanly
        // when the slot refuses (rather than attempt an unsuccessful moveItemStackTo that
        // would still let the player shift-click elsewhere).
        if (!elytraSlot.mayPlace(stackInSource)) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] quickMove IN elytraSlot.mayPlace=false — falling through to vanilla"
            );
            return;
        }

        ItemStack preMoveSnapshot = stackInSource.copy();
        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] quickMove IN beforeMove={} elytraIdx={} srcIdx={}",
            stackInSource, elytraIdx, index
        );

        boolean moved = this.moveItemStackTo(stackInSource, elytraIdx, elytraIdx + 1, false);
        if (!moved) {
            // Slot refused (e.g. already full). Let vanilla handle the move instead of
            // short-circuiting — the player might still want to move to inventory / hotbar.
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] quickMove IN moveItemStackTo=false — falling through to vanilla"
            );
            return;
        }

        if (stackInSource.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY, preMoveSnapshot);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, stackInSource);
        cir.setReturnValue(preMoveSnapshot);
        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] quickMove IN SUCCESS pre={} remainingInSource={}",
            preMoveSnapshot, sourceSlot.getItem()
        );
    }

    /**
     * Find the custom elytra-slot index by identifying the slot whose container is an
     * {@link ElytraSlotContainer} owned by this player. Returns -1 if not present.
     */
    private static int findElytraSlotIndex(InventoryMenu menu, Player player) {
        ElytraSlotContainer expected = ((IElytraSlotPlayer) player).elytraslot_getElytraContainer();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s != null && s.container == expected) return i;
        }
        return -1;
    }
}
