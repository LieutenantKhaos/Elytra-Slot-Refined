package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraHolder;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
public class HumanoidRenderStateMixin {

    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void onExtractHumanoidRenderState(
        LivingEntity entity,
        HumanoidRenderState state,
        float partialTick,
        net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver,
        CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;
        if (!(state instanceof IElytraHolder holder)) return;

        holder.elytraslot_setAccessoriesElytra(ItemStack.EMPTY);

        ItemStack elytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (ElytraSlotUtil.isElytraLike(elytra)) {
            holder.elytraslot_setAccessoriesElytra(elytra.copy());
        }
    }
}
