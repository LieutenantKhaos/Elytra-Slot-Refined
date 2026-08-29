package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.client.ElytraRenderHolder;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public abstract class HumanoidRenderStateMixin implements ElytraRenderHolder {

    @Unique
    private ItemStack elytraslot$elytra = ItemStack.EMPTY;

    @Override
    public ItemStack elytraslot$getElytra() {
        return this.elytraslot$elytra;
    }

    @Override
    public void elytraslot$setElytra(ItemStack stack) {
        this.elytraslot$elytra = stack;
    }
}
