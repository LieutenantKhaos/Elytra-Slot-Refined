package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.host.LegacyElytraCarrier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One-time world migration from Elytra Slot 2.x, which stored the equipped elytra
 * under a custom "elytraslot_item" player NBT key. 3.x keeps it in the vanilla
 * BODY equipment slot, which vanilla saves, so the legacy stack is moved there on
 * load and the legacy key is simply not written back.
 *
 * <p>If it cannot be placed (BODY occupied and inventory full) the stack is kept
 * under the legacy key instead of being dropped, so the migration retries on the
 * next load and the item is never lost.
 */
@Mixin(Player.class)
public abstract class PlayerLegacyDataMixin implements LegacyElytraCarrier {

    @Unique
    private static final String LEGACY_KEY = "elytraslot_item";

    @Unique
    private ItemStack elytraslot$pendingLegacyElytra = ItemStack.EMPTY;

    @Override
    public ItemStack elytraslot$getPendingLegacyElytra() {
        return this.elytraslot$pendingLegacyElytra;
    }

    @Override
    public void elytraslot$setPendingLegacyElytra(ItemStack stack) {
        this.elytraslot$pendingLegacyElytra = stack;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$migrateLegacyElytra(ValueInput input, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        this.elytraslot$pendingLegacyElytra = ItemStack.EMPTY;
        input.read(LEGACY_KEY, ItemStack.CODEC).ifPresent(stack -> {
            if (stack.isEmpty()) {
                return;
            }
            if (self.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
                // Write through the inventory view: no equip sound or game event on load.
                self.getInventory().setItem(Inventory.SLOT_BODY_ARMOR, stack);
            } else if (!self.getInventory().add(stack)) {
                this.elytraslot$pendingLegacyElytra = stack;
            }
        });
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$keepUnplaceableLegacyElytra(ValueOutput output, CallbackInfo ci) {
        if (!this.elytraslot$pendingLegacyElytra.isEmpty()) {
            output.store(LEGACY_KEY, ItemStack.CODEC, this.elytraslot$pendingLegacyElytra);
        }
    }
}
