package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a persistent 1-slot container to Player for elytra storage.
 * Data is saved/loaded via the new ValueOutput/ValueInput API in MC 26.1.
 */
@Mixin(Player.class)
public class PlayerElytraStorageMixin implements IElytraSlotPlayer {

    @Unique
    private final SimpleContainer elytraslot_container = new SimpleContainer(1);

    @Override
    public SimpleContainer elytraslot_getElytraContainer() {
        return this.elytraslot_container;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$saveData(ValueOutput output, CallbackInfo ci) {
        ItemStack stack = this.elytraslot_container.getItem(0);
        if (!stack.isEmpty()) {
            output.store("elytraslot_item", ItemStack.CODEC, stack);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void elytraslot$loadData(ValueInput input, CallbackInfo ci) {
        input.read("elytraslot_item", ItemStack.CODEC).ifPresent(stack -> {
            this.elytraslot_container.setItem(0, stack);
        });
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void elytraslot$dropOnDeath(ServerLevel level, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack stack = this.elytraslot_container.getItem(0);
        if (!stack.isEmpty()) {
            player.drop(stack, true, false);
            this.elytraslot_container.setItem(0, ItemStack.EMPTY);
        }
    }
}
