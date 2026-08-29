package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.client.ElytraRenderHolder;
import com.warwa.elytraslot.client.EquippedElytraRender;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Populates the render-state elytra holder during extraction (players reach this
 * through AvatarRenderer as well). The holder is reset first — render states are
 * reused, and skipping the reset leaves ghost wings after unequip.
 */
@Mixin(HumanoidMobRenderer.class)
public abstract class HumanoidMobRendererMixin {

    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void elytraslot$captureElytra(LivingEntity entity, HumanoidRenderState state,
                                                 float partialTick, ItemModelResolver itemModelResolver,
                                                 CallbackInfo ci) {
        if (!(state instanceof ElytraRenderHolder holder)) {
            return;
        }
        holder.elytraslot$setElytra(ItemStack.EMPTY);
        if (entity instanceof Player player) {
            ItemStack elytra = EquippedElytraRender.resolve(player);
            if (!elytra.isEmpty()) {
                holder.elytraslot$setElytra(elytra.copy());
            }
        }
    }
}
