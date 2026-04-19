package com.warwa.elytraslot.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.IElytraHolder;
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
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the custom-slot elytra's wings when the player's real chest slot has no
 * elytra-like item. The call to {@code equipmentRenderer.renderLayers(...)} mirrors
 * vanilla {@link WingsLayer#submit} exactly:
 * <ul>
 *   <li>Z-translation of {@code 0.125F} (same as vanilla).</li>
 *   <li>Player skin / cape texture looked up via an inlined copy of vanilla's private
 *       {@code getPlayerElytraTexture(state)} helper — so custom capes render on the
 *       custom-slot elytra just like a chest-slot elytra.</li>
 *   <li>Tint color passed as {@code 0} (vanilla literal) — vanilla does not apply
 *       {@code DYED_COLOR} tint to elytras.</li>
 * </ul>
 */
@Mixin(WingsLayer.class)
public class ElytraLayerMixin<S extends HumanoidRenderState, M extends EntityModel<S>> {

    @Shadow private ElytraModel elytraModel;
    @Shadow private ElytraModel elytraBabyModel;
    @Shadow private EquipmentLayerRenderer equipmentRenderer;

    /**
     * Injected at every RETURN opcode (not only TAIL). Vanilla {@code WingsLayer.submit}
     * has an early return when {@code state.chestEquipment} has no {@code Equippable}
     * component or its {@code assetId} is empty — i.e. when the vanilla chest slot has
     * no elytra. That early return is the common case for custom-slot-only usage, and
     * {@code @At("TAIL")} does NOT fire there because Mixin's TAIL targets only the
     * final return opcode in the method. Using {@code @At("RETURN")} injects before
     * every return, so our custom-slot elytra renders regardless of which path vanilla
     * took.
     */
    @Inject(method = "submit", at = @At("RETURN"))
    private void onSubmit(PoseStack poseStack, SubmitNodeCollector collector, int light, S renderState, float yRot, float xRot, CallbackInfo ci) {
        if (!(renderState instanceof IElytraHolder holder)) return;
        ItemStack elytra = holder.elytraslot_getAccessoriesElytra();
        if (elytra.isEmpty()) return;
        // Don't render if chest already has elytra — vanilla's submit body already drew it.
        if (ElytraSlotUtil.isElytraLike(renderState.chestEquipment)) return;

        Equippable equippable = elytra.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) return;

        // Mirror vanilla WingsLayer.submit exactly.
        Identifier playerElytraTexture = getPlayerElytraTexture(renderState);
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
            playerElytraTexture,
            renderState.outlineColor,
            0
        );
        poseStack.popPose();
    }

    /**
     * Inlined copy of vanilla {@code WingsLayer.getPlayerElytraTexture} (package-private
     * there). Returns the player's skin-defined elytra texture if present, otherwise the
     * cape texture if the cape is shown, otherwise null.
     */
    private static @Nullable Identifier getPlayerElytraTexture(HumanoidRenderState state) {
        if (state instanceof AvatarRenderState playerState) {
            PlayerSkin skin = playerState.skin;
            if (skin.elytra() != null) {
                return skin.elytra().texturePath();
            }
            if (skin.cape() != null && playerState.showCape) {
                return skin.cape().texturePath();
            }
        }
        return null;
    }
}
