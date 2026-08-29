package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.host.ElytraHost;
import com.warwa.elytraslot.host.ElytraHosts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flight support for the two slots vanilla can't see on its own.
 *
 * <p>BODY (builtin slot): vanilla's canGlide/updateFallFlying already iterate the
 * BODY equipment slot — the only failing check is {@code canGlideUsing}'s
 * "item equips to this slot" comparison, so one RETURN tweak there routes the
 * entire vanilla flight pipeline (activation, 10-tick glide events, 20-tick
 * durability with vanilla RNG, break handling) through untouched vanilla code.
 *
 * <p>External host slots without native flight (Curios): canGlide is extended and
 * updateFallFlying is mirrored with the host's stack added to the vanilla damage
 * pool. Hosts that manage flight themselves (Trinkets) are left alone.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGlideMixin {

    @Shadow
    protected int fallFlyTicks;

    @Shadow
    protected abstract boolean canGlide();

    /** Chest-equippable gliders count as gliders while sitting in the BODY slot. */
    @Inject(method = "canGlideUsing", at = @At("RETURN"), cancellable = true)
    private static void elytraslot$glideFromBodySlot(ItemStack stack, EquipmentSlot slot,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || slot != EquipmentSlot.BODY) {
            return;
        }
        if (!stack.has(DataComponents.GLIDER) || stack.nextDamageWillBreak()) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
            cir.setReturnValue(true);
        }
    }

    /**
     * A usable glider in an external host slot (Curios) activates gliding. The
     * vanilla gates are re-checked because a false return can mean either
     * "gate blocked" or "no glider".
     */
    /**
     * Explicit descriptor: NeoForge adds a {@code canGlide(boolean)} overload, and a
     * bare method name binds to only the first match — which silently left the
     * client-side check unpatched. On NeoForge gliding is driven by the
     * {@code neoforge:gliding_flight} attribute instead, so this inject matters only
     * on loaders that still scan equipment.
     */
    @Inject(method = "canGlide()Z", at = @At("RETURN"), cancellable = true)
    private void elytraslot$glideFromHostSlot(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)
            || self.onGround() || self.isPassenger() || self.hasEffect(MobEffects.LEVITATION)) {
            return;
        }
        ItemStack hostStack = hostFlightStack(player);
        if (!hostStack.isEmpty() && !hostStack.nextDamageWillBreak()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Mirror of vanilla updateFallFlying with the external host's glider joining
     * the damage pool. Only replaces vanilla while such a glider is present;
     * with the elytra in BODY or chest, vanilla runs untouched.
     */
    @Inject(method = "updateFallFlying()V", at = @At("HEAD"), cancellable = true)
    private void elytraslot$updateFallFlyingWithHostGlider(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        ItemStack hostStack = hostFlightStack(player);
        if (hostStack.isEmpty()) {
            return;
        }
        ci.cancel();

        self.checkFallDistanceAccumulation();
        if (self.level().isClientSide()) {
            return;
        }
        if (!canGlide()) {
            ((EntityAccessor) self).elytraslot$setSharedFlag(7, false);
            return;
        }
        int ticks = this.fallFlyTicks + 1;
        if (ticks % 10 == 0) {
            if ((ticks / 10) % 2 == 0) {
                List<EquipmentSlot> sources = new ArrayList<>();
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    if (LivingEntity.canGlideUsing(self.getItemBySlot(slot), slot)) {
                        sources.add(slot);
                    }
                }
                if (!hostStack.nextDamageWillBreak()) {
                    sources.add(null); // null = the external host's slot
                }
                if (!sources.isEmpty()) {
                    EquipmentSlot picked = Util.getRandom(sources, self.getRandom());
                    if (picked == null) {
                        ElytraHosts.forSide(player).hurt(player, 1);
                    } else {
                        self.getItemBySlot(picked).hurtAndBreak(1, self, picked);
                    }
                }
            }
            self.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
    }

    /** The glider in an external, non-flight-managing host slot, or EMPTY. */
    private static ItemStack hostFlightStack(Player player) {
        ElytraHost host = ElytraHosts.forSide(player);
        if (host.isBuiltin() || host.managesFlight()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = host.get(player);
        return Gliders.isChestGlider(stack) ? stack : ItemStack.EMPTY;
    }
}
