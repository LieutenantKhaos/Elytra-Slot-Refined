package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Equippable.class, priority = 500)
public class ElytraEquipMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void onSwapWithEquipmentSlot(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ElytraSlotUtil.isElytraLike(stack)) return;

        // Block if chest already has elytra
        if (ElytraSlotUtil.isElytraLike(player.getItemBySlot(EquipmentSlot.CHEST))) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        IElytraSlotPlayer slotPlayer = (IElytraSlotPlayer) player;
        ItemStack existing = slotPlayer.elytraslot_getElytraStack();
        ItemStack toEquip = stack.getCount() <= 1 ? stack.copyAndClear() : stack.consumeAndReturn(1, player);

        slotPlayer.elytraslot_setElytraStack(toEquip);

        if (!existing.isEmpty()) {
            if (!player.getInventory().add(existing)) {
                player.drop(existing, false);
            }
        }

        if (!player.level().isClientSide()) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(toEquip.getItem()));
        }

        Equippable equippable = toEquip.get(DataComponents.EQUIPPABLE);
        SoundEvent soundEvent = equippable != null
            ? equippable.equipSound().value()
            : SoundEvents.ARMOR_EQUIP_ELYTRA.value();
        player.level().playSound(
            player,
            player.getX(), player.getY(), player.getZ(),
            soundEvent,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
