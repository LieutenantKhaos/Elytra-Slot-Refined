package com.warwa.elytraslot.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipMixin {

    /**
     * onEquipItem plays the equip sound only when the item's equippable slot
     * matches the slot it entered. Chest-equippable gliders entering BODY (our
     * slot) should sound exactly like entering the chest slot, so the comparison
     * treats them as matching. All other onEquipItem behavior (guards, game
     * events) stays vanilla.
     */
    @ModifyExpressionValue(
        method = "onEquipItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/equipment/Equippable;slot()Lnet/minecraft/world/entity/EquipmentSlot;"))
    private EquipmentSlot elytraslot$equipSoundInBodySlot(EquipmentSlot original, EquipmentSlot slot,
                                                          ItemStack oldItem, ItemStack newItem) {
        return original == EquipmentSlot.CHEST && slot == EquipmentSlot.BODY
            && Gliders.isGlider(newItem) ? EquipmentSlot.BODY : original;
    }

    /**
     * Players emit equip vibrations only for humanoid-armor slots, and BODY is an
     * animal-armor slot — so without this, equipping the elytra in our slot would
     * be silent to sculk sensors while chest-equipping it is not.
     */
    @ModifyExpressionValue(
        method = "onEquipItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;doesEmitEquipEvent(Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
    private boolean elytraslot$emitEquipEventForBodyGlider(boolean original, EquipmentSlot slot,
                                                           ItemStack oldItem, ItemStack newItem) {
        if (original || slot != EquipmentSlot.BODY || !((Object) this instanceof Player)) {
            return original;
        }
        return Gliders.isGlider(newItem) || Gliders.isGlider(oldItem);
    }

    /**
     * Exclusivity choke point: this single method backs ArmorSlot.mayPlace (GUI
     * placement into the chest slot) and entity-equip paths, so vetoing it here
     * prevents a second glider on the chest while one sits in BODY or in the
     * active external host's slot.
     */
    @Inject(method = "isEquippableInSlot", at = @At("RETURN"), cancellable = true)
    private void elytraslot$blockSecondGlider(ItemStack stack, EquipmentSlot slot,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || slot != EquipmentSlot.CHEST || !Gliders.isGlider(stack)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && elytraslot$wearsGliderElsewhere(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Dispensers equip through their own gate rather than isEquippableInSlot, so
     * the same veto is applied here — otherwise a dispenser could put a second
     * elytra on the chest of a player already wearing one in our slot.
     */
    @Inject(method = "canEquipWithDispenser", at = @At("RETURN"), cancellable = true)
    private void elytraslot$blockDispenserSecondGlider(ItemStack stack,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !Gliders.isChestGlider(stack)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && elytraslot$wearsGliderElsewhere(player)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean elytraslot$wearsGliderElsewhere(Player player) {
        if (Gliders.isGlider(player.getItemBySlot(EquipmentSlot.BODY))) {
            return true;
        }
        ElytraHost host = ElytraHosts.forSide(player);
        return !host.isBuiltin() && Gliders.isGlider(host.get(player));
    }
}
