package com.warwa.elytraslot.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enchantment parity for an elytra in an external host slot whose host mod does
 * not feed its stacks into vanilla's equipment-enchantment iteration (Curios).
 *
 * <p>runIterationOnEquipment is the choke point behind tickEffects, damage
 * protection, post-attack effects, and location-changed effects. It iterates
 * every EquipmentSlot including BODY, which is necessarily empty while an
 * external host holds the elytra (exclusivity), so the host's stack is
 * substituted into the BODY position of the loop — vanilla then processes it
 * exactly like worn equipment.
 *
 * <p>In builtin mode the elytra IS the BODY equipment and vanilla needs no help;
 * Trinkets feeds its own slots into these methods natively.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @WrapOperation(
        method = "runIterationOnEquipment",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack elytraslot$includeHostGlider(LivingEntity entity, EquipmentSlot slot,
                                                          Operation<ItemStack> original) {
        ItemStack stack = original.call(entity, slot);
        if (slot != EquipmentSlot.BODY || !stack.isEmpty() || !(entity instanceof Player player)) {
            return stack;
        }
        ElytraHost host = ElytraHosts.forSide(player);
        if (host.isBuiltin() || host.managesEnchantmentEffects()) {
            return stack;
        }
        ItemStack hostStack = host.get(player);
        return Gliders.isGlider(hostStack) ? hostStack : stack;
    }
}
