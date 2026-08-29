package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-click-to-equip: routes a chest-equippable glider in hand into the BODY
 * slot instead of the chest. The body is an exact mirror of vanilla
 * swapWithEquipmentSlot (verified against the 26.2 bytecode) with BODY as the
 * target; writes go through setItemSlot so vanilla onEquipItem fires the game
 * event and — via LivingEntityEquipMixin — the equip sound.
 *
 * <p>Deference rules: a glider already on the chest keeps vanilla's chest-swap
 * behavior; an external host that equips-on-use itself (Trinkets, Curios)
 * handles the click before this method unless our BODY panel slot holds the
 * elytra, in which case the swap targets BODY.
 */
@Mixin(value = Equippable.class, priority = 500)
public abstract class EquippableSwapMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void elytraslot$swapIntoElytraSlot(ItemStack stack, Player player,
                                               CallbackInfoReturnable<InteractionResult> cir) {
        Equippable self = (Equippable) (Object) this;
        if (self.slot() != EquipmentSlot.CHEST || !Gliders.isGlider(stack)) {
            return;
        }
        if (Gliders.isGlider(player.getItemBySlot(EquipmentSlot.CHEST))) {
            return; // vanilla chest swap
        }
        ElytraHost host = ElytraHosts.forSide(player);
        boolean bodyHoldsGlider = Gliders.isGlider(player.getItemBySlot(EquipmentSlot.BODY));
        if (!host.isBuiltin() && !bodyHoldsGlider) {
            return; // the external host's own equip-on-use pipeline owns this click
        }

        // Vanilla outer gate (slot-independent parts).
        if (!player.canUseSlot(EquipmentSlot.CHEST) || !self.canBeEquippedBy(player.typeHolder())) {
            return; // vanilla returns PASS; falling through yields the same result
        }
        ItemStack equipped = player.getItemBySlot(EquipmentSlot.BODY);
        if ((EnchantmentHelper.has(equipped, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
                && !player.isCreative())
            || ItemStack.isSameItemSameComponents(stack, equipped)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!player.level().isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }

        if (stack.getCount() <= 1) {
            ItemStack toHand = equipped.isEmpty() ? stack : equipped.copyAndClear();
            ItemStack toEquip = player.isCreative() ? stack.copy() : stack.copyAndClear();
            player.setItemSlot(EquipmentSlot.BODY, toEquip);
            cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(toHand));
        } else {
            ItemStack displaced = equipped.copyAndClear();
            ItemStack toEquip = stack.consumeAndReturn(1, player);
            player.setItemSlot(EquipmentSlot.BODY, toEquip);
            if (!player.getInventory().add(displaced)) {
                player.drop(displaced, false);
            }
            cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(stack));
        }
    }
}
