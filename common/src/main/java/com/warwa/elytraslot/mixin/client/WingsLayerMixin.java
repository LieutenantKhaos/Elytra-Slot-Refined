package com.warwa.elytraslot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.client.ElytraRenderHolder;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the slot-equipped elytra exactly like vanilla renders a chest-equipped
 * one. Injected at every RETURN — vanilla early-returns when chestEquipment has
 * no wings asset, which is precisely the slot-equipped case, so TAIL would never
 * fire there.
 *
 * <p>The player-texture helper is a namespaced @Unique copy of vanilla's
 * package-private getPlayerElytraTexture: a same-name helper would be
 * mixin-merged OVER the vanilla method and crash other mods injecting into it.
 */
@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin<S extends HumanoidRenderState, M extends EntityModel<S>> {

    @Shadow
    @Final
    private ElytraModel elytraModel;

    @Shadow
    @Final
    private ElytraModel elytraBabyModel;

    @Shadow
    @Final
    private EquipmentLayerRenderer equipmentRenderer;

    @Inject(method = "submit", at = @At("RETURN"))
    private void elytraslot$submitSlotElytra(PoseStack poseStack, SubmitNodeCollector collector,
                                             int packedLight, S renderState, float yRot, float xRot,
                                             CallbackInfo ci) {
        if (!(renderState instanceof ElytraRenderHolder holder)) {
            return;
        }
        ItemStack elytra = holder.elytraslot$getElytra();
        if (elytra.isEmpty() || Gliders.isGlider(renderState.chestEquipment)) {
            return; // nothing to draw, or vanilla already drew chest wings
        }
        Equippable equippable = elytra.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return;
        }
        ElytraModel model = renderState.isBaby ? this.elytraBabyModel : this.elytraModel;
        Identifier texture = elytraslot$playerElytraTexture(renderState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.WINGS,
            equippable.assetId().get(), model, renderState, elytra, poseStack, collector,
            packedLight, texture, renderState.outlineColor, 0);
        poseStack.popPose();
    }

    /** Mirror of vanilla WingsLayer.getPlayerElytraTexture (custom elytra/cape textures). */
    @Unique
    @Nullable
    private static Identifier elytraslot$playerElytraTexture(HumanoidRenderState state) {
        if (state instanceof AvatarRenderState avatarState) {
            if (avatarState.skin.elytra() != null) {
                return avatarState.skin.elytra().texturePath();
            }
            if (avatarState.skin.cape() != null && avatarState.showCape) {
                return avatarState.skin.cape().texturePath();
            }
        }
        return null;
    }
}
