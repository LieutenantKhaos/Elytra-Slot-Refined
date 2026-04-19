package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraHolder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@link CapeLayer#submit} hides the cape when the player's chest equipment is
 * a glider (WINGS layer type) or humanoid armor — it inspects {@code state.chestEquipment}.
 * Our custom slot elytra is not placed into {@code state.chestEquipment} (it lives on
 * our separate {@link IElytraHolder} field), so vanilla's cape-hide logic doesn't fire
 * for a custom-slot-equipped elytra and the cape renders through the elytra, which is
 * not what vanilla does when a chest-slot elytra is equipped.
 *
 * This injection short-circuits {@code CapeLayer.submit} at HEAD when the render-state
 * holder carries a custom-slot elytra, matching vanilla's behavior of hiding the cape
 * whenever an elytra is worn.
 */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void elytraslot$hideCapeForCustomElytra(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        AvatarRenderState state,
        float yRot,
        float xRot,
        CallbackInfo ci
    ) {
        if (state instanceof IElytraHolder holder
            && ElytraSlotUtil.isElytraLike(holder.elytraslot_getAccessoriesElytra())) {
            ci.cancel();
        }
    }
}
