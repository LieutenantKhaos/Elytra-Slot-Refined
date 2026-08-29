package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Companion to EnchantmentHelperMixin for the third equipment-iteration site:
 * Enchantment.getSlotItems backs getEnchantmentLevel (e.g. looting/efficiency
 * lookups on equipment). Adds the external host's elytra at the BODY key when
 * that key is free and the enchantment applies to BODY.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "getSlotItems", at = @At("RETURN"))
    private void elytraslot$includeHostGlider(LivingEntity entity,
                                              CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ElytraHost host = ElytraHosts.forSide(player);
        if (host.isBuiltin() || host.managesEnchantmentEffects()) {
            return;
        }
        Map<EquipmentSlot, ItemStack> items = cir.getReturnValue();
        if (items.containsKey(EquipmentSlot.BODY)
            || !((Enchantment) (Object) this).matchingSlot(EquipmentSlot.BODY)) {
            return;
        }
        ItemStack hostStack = host.get(player);
        if (Gliders.isGlider(hostStack)) {
            items.put(EquipmentSlot.BODY, hostStack);
        }
    }
}
