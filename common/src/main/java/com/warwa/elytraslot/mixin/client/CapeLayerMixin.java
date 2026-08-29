package com.warwa.elytraslot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.client.ElytraRenderHolder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla hides the cape by looking at chestEquipment, which a slot-equipped
 * elytra never populates — without this the cape clips through the wings.
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void elytraslot$hideCapeUnderElytra(PoseStack poseStack, SubmitNodeCollector collector,
                                                int packedLight, AvatarRenderState renderState,
                                                float yRot, float xRot, CallbackInfo ci) {
        if (renderState instanceof ElytraRenderHolder holder
            && Gliders.isGlider(holder.elytraslot$getElytra())) {
            ci.cancel();
        }
    }
}
