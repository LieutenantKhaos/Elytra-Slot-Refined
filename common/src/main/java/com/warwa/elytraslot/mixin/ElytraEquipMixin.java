package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraEquipEffects;
import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link Equippable#swapWithEquipmentSlot(ItemStack, Player)} to route
 * elytras into the custom slot with full vanilla parity:
 * <ul>
 *   <li>Only intercepts when the in-hand stack's {@code Equippable.slot()} is {@code CHEST}
 *       (B1 fix — non-CHEST-slotted gliders fall through to vanilla).</li>
 *   <li>Only intercepts when the real chest slot does NOT already have an elytra-like
 *       item (otherwise vanilla's chest↔hand swap runs normally).</li>
 *   <li>Mirrors vanilla's outer gate: {@code canUseSlot(CHEST) && canBeEquippedBy(typeHolder)}
 *       → {@code PASS} otherwise.</li>
 *   <li>Mirrors vanilla's inner gate: {@code PREVENT_ARMOR_CHANGE + !creative} or
 *       {@code isSameItemSameComponents} → {@code FAIL}.</li>
 *   <li>Fires the same {@code awardStat(ITEM_USED)} server-side.</li>
 *   <li>Fires the equip sound + {@link net.minecraft.world.level.gameevent.GameEvent#EQUIP}
 *       via {@link ElytraEquipEffects#onSlotChanged}.</li>
 *   <li>Returns {@code InteractionResult.SUCCESS.heldItemTransformedTo(...)} in the
 *       correct single-count vs stacked branch, matching vanilla's return value.</li>
 * </ul>
 */
@Mixin(value = Equippable.class, priority = 500)
public abstract class ElytraEquipMixin {

    @Shadow public abstract boolean canBeEquippedBy(Holder<EntityType<?>> type);

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void onSwapWithEquipmentSlot(ItemStack inHand, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ElytraSlotUtil.isElytraLike(inHand)) return;

        // B1 fix: only route CHEST-slotted gliders. Modded / non-CHEST gliders fall through
        // so vanilla handles them in their native slot.
        Equippable inHandEquippable = inHand.get(DataComponents.EQUIPPABLE);
        if (inHandEquippable == null || inHandEquippable.slot() != EquipmentSlot.CHEST) return;

        // G2 fix: honor Equippable.swappable() flag. Vanilla's `Item.use` checks this
        // before calling swapWithEquipmentSlot, but a mod could bypass Item.use and
        // call the swap directly. Defensive check.
        if (!inHandEquippable.swappable()) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] ElytraEquipMixin.onSwap gate-trip swappable=false stack={}", inHand
            );
            return;
        }

        // If the vanilla chest slot already has an elytra, do not redirect — let vanilla
        // swap the chest-elytra with the in-hand elytra exactly as vanilla does.
        if (ElytraSlotUtil.isElytraLike(player.getItemBySlot(EquipmentSlot.CHEST))) {
            return;
        }

        // Mirror vanilla outer gate: canUseSlot + canBeEquippedBy. If either fails, PASS.
        if (!player.canUseSlot(EquipmentSlot.CHEST)
            || !this.canBeEquippedBy(player.typeHolder())) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        IElytraSlotPlayer slotPlayer = (IElytraSlotPlayer) player;
        ItemStack inEquipmentSlot = slotPlayer.elytraslot_getElytraStack();

        // Mirror vanilla inner gate:
        //   PREVENT_ARMOR_CHANGE enchantment blocks non-creative swap
        //   identical item+components: no swap
        if (EnchantmentHelper.has(inEquipmentSlot, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            && !player.isCreative()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (ItemStack.isSameItemSameComponents(inHand, inEquipmentSlot)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] swapWithEquipmentSlot inHand={} existing={} count={} creative={}",
            inHand, inEquipmentSlot, inHand.getCount(), player.isCreative()
        );

        if (!player.level().isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
        }

        // Snapshot the previous stack BEFORE mutation so ElytraEquipEffects sees the
        // correct old/new pair.
        ItemStack oldForEffects = inEquipmentSlot.copy();

        InteractionResult result;
        if (inHand.getCount() <= 1) {
            ItemStack swappedToHand = inEquipmentSlot.isEmpty() ? inHand : inEquipmentSlot.copyAndClear();
            ItemStack swappedToEquipment = player.isCreative() ? inHand.copy() : inHand.copyAndClear();
            slotPlayer.elytraslot_setElytraStack(swappedToEquipment);
            ElytraEquipEffects.onSlotChanged(player, oldForEffects, swappedToEquipment);
            result = InteractionResult.SUCCESS.heldItemTransformedTo(swappedToHand);
        } else {
            ItemStack swappedToInventory = inEquipmentSlot.copyAndClear();
            ItemStack swappedToEquipment = inHand.consumeAndReturn(1, player);
            slotPlayer.elytraslot_setElytraStack(swappedToEquipment);
            ElytraEquipEffects.onSlotChanged(player, oldForEffects, swappedToEquipment);
            if (!swappedToInventory.isEmpty() && !player.getInventory().add(swappedToInventory)) {
                player.drop(swappedToInventory, false);
            }
            result = InteractionResult.SUCCESS.heldItemTransformedTo(inHand);
        }
        cir.setReturnValue(result);
    }
}
