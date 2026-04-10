package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class ElytraSlotMixin {

    @Inject(method = "canGlide", at = @At("RETURN"), cancellable = true)
    private void onCanGlide(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!(((Object) this) instanceof Player player)) return;
        if (player.onGround()) return;
        if (player.isPassenger()) return;
        if (player.hasEffect(MobEffects.LEVITATION)) return;

        ItemStack elytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (ElytraSlotUtil.isElytraLike(elytra) && LivingEntity.canGlideUsing(elytra, EquipmentSlot.CHEST)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void onUpdateFallFlying(CallbackInfo ci) {
        if (!(((Object) this) instanceof Player player)) return;
        if (!player.isFallFlying()) return;

        ItemStack elytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (!ElytraSlotUtil.isElytraLike(elytra)) return;

        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.onGround() || entity.isInWater() || entity.hasEffect(MobEffects.LEVITATION)) {
            entity.stopFallFlying();
            ci.cancel();
            return;
        }

        if (LivingEntity.canGlideUsing(elytra, EquipmentSlot.CHEST)) {
            entity.checkFallDistanceAccumulation();
            int ticks = entity.getFallFlyingTicks() + 1;
            if (ticks % 10 == 0 && ticks / 10 % 2 == 0) {
                elytra.hurtAndBreak(1, player, EquipmentSlot.CHEST);
            }
            ci.cancel();
        } else {
            entity.stopFallFlying();
            ci.cancel();
        }
    }
}
