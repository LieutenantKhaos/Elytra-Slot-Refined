package com.warwa.elytraslot.mixin.client;

import com.warwa.elytraslot.IElytraHolder;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateAccessorMixin implements IElytraHolder {
    @Unique
    private ItemStack elytraslot_accessoriesElytra = ItemStack.EMPTY;

    @Override
    public ItemStack elytraslot_getAccessoriesElytra() {
        return this.elytraslot_accessoriesElytra;
    }

    @Override
    public void elytraslot_setAccessoriesElytra(ItemStack stack) {
        this.elytraslot_accessoriesElytra = stack;
    }
}
