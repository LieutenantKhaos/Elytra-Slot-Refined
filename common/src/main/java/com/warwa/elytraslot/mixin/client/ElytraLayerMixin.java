package com.warwa.elytraslot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraHolder;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
public class ElytraLayerMixin<S extends HumanoidRenderState, M extends EntityModel<S>> {

    @Shadow private ElytraModel elytraModel;
    @Shadow private ElytraModel elytraBabyModel;
    @Shadow private EquipmentLayerRenderer equipmentRenderer;

    @Inject(method = "submit", at = @At("TAIL"))
    private void onSubmit(PoseStack poseStack, SubmitNodeCollector collector, int light, S renderState, float yRot, float xRot, CallbackInfo ci) {
        if (!(renderState instanceof IElytraHolder holder)) return;
        ItemStack elytra = holder.elytraslot_getAccessoriesElytra();
        if (elytra.isEmpty()) return;
        // Don't render if chest already has elytra (vanilla handles that)
        if (ElytraSlotUtil.isElytraLike(renderState.chestEquipment)) return;

        Equippable equippable = elytra.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) return;

        int color = 0;
        DyedItemColor dyedColor = elytra.get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            color = dyedColor.rgb();
        }

        ElytraModel model = renderState.isBaby ? this.elytraBabyModel : this.elytraModel;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.equipmentRenderer.renderLayers(
            EquipmentClientInfo.LayerType.WINGS,
            equippable.assetId().get(),
            model,
            renderState,
            elytra,
            poseStack,
            collector,
            light,
            null,
            renderState.outlineColor,
            color
        );
        poseStack.popPose();
    }
}
