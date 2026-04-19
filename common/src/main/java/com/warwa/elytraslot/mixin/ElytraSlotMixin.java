package com.warwa.elytraslot.mixin;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotUtil;
import com.warwa.elytraslot.GliderSource;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class ElytraSlotMixin {

    @Shadow protected abstract boolean canGlide();

    @Inject(method = "canGlide", at = @At("RETURN"), cancellable = true)
    private void onCanGlide(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!(((Object) this) instanceof Player player)) return;
        // Mirror vanilla canGlide gate conditions before adding the custom slot's stack.
        if (player.onGround()) return;
        if (player.isPassenger()) return;
        if (player.hasEffect(MobEffects.LEVITATION)) return;

        ItemStack elytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (ElytraSlotUtil.isElytraLike(elytra) && LivingEntity.canGlideUsing(elytra, EquipmentSlot.CHEST)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void onUpdateFallFlying(CallbackInfo ci) {
        if (!(((Object) this) instanceof Player player)) return;
        // Only intercept when the custom slot supplies the glider. If the chest has a real
        // elytra, let vanilla run unmodified.
        if (ElytraSlotUtil.isElytraLike(player.getItemBySlot(EquipmentSlot.CHEST))) return;

        ItemStack elytra = ((IElytraSlotPlayer) player).elytraslot_getElytraStack();
        if (!ElytraSlotUtil.isElytraLike(elytra)) return;

        LivingEntity entity = (LivingEntity) (Object) this;

        // Exact mirror of vanilla LivingEntity.updateFallFlying:
        //   checkFallDistanceAccumulation();
        //   if (!level().isClientSide()) {
        //       if (!canGlide()) { setSharedFlag(7, false); return; }
        //       int t = fallFlyTicks + 1;
        //       if (t % 10 == 0) {
        //           if ((t / 10) % 2 == 0) <damage RANDOM glider slot>
        //           gameEvent(GameEvent.ELYTRA_GLIDE);
        //       }
        //   }
        entity.checkFallDistanceAccumulation();
        if (!entity.level().isClientSide()) {
            if (!this.canGlide()) {
                ((LivingEntityAccessor) entity).elytraslot$setSharedFlag(7, false);
                ci.cancel();
                return;
            }

            int checkFallFlyTicks = entity.getFallFlyingTicks() + 1;
            if (checkFallFlyTicks % 10 == 0) {
                int freeFallInterval = checkFallFlyTicks / 10;
                if (freeFallInterval % 2 == 0) {
                    // A2 fix: iterate EquipmentSlot.VALUES in the SAME order vanilla does,
                    // substituting our custom slot in the CHEST position so the random
                    // pick is order-equivalent to vanilla's.
                    List<GliderSource> sources = new ArrayList<>();
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        if (slot == EquipmentSlot.CHEST) {
                            // Chest is empty here (we gated on that above); the custom slot
                            // stands in for CHEST.
                            sources.add(new GliderSource(elytra, EquipmentSlot.CHEST, true));
                        } else {
                            ItemStack stack = entity.getItemBySlot(slot);
                            if (LivingEntity.canGlideUsing(stack, slot)) {
                                sources.add(new GliderSource(stack, slot, false));
                            }
                        }
                    }

                    GliderSource pick = Util.getRandom(sources, entity.getRandom());
                    ElytraSlotConstants.LOGGER.info(
                        "[elytraslot] updateFallFlying damage pick={} sources={} player={}",
                        pick, sources.size(), player.getName().getString()
                    );
                    damageGlider(pick, player);
                }
                entity.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
        ci.cancel();
    }

    private static void damageGlider(GliderSource source, Player player) {
        if (!source.fromCustomSlot()) {
            // Vanilla-equipped glider (e.g., a modded glider in LEGS); delegate to the
            // standard slot-aware path which invokes onEquippedItemBroken naturally.
            source.stack().hurtAndBreak(1, player, source.slot());
            return;
        }
        // Custom-slot glider: use the low-level overload with a custom onBreak that mirrors
        // vanilla's onEquippedItemBroken exactly, EXCEPT we scope stopLocationBasedEffects
        // to the broken stack itself (not getItemBySlot(CHEST), which in our case holds a
        // separate chestplate and whose modifiers must not be stripped).
        if (player.level() instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            // Snapshot the pre-break stack so we can iterate its modifiers AFTER the
            // hurtAndBreak has shrunk the live stack to AIR.
            ItemStack preBreakSnapshot = source.stack().copy();
            source.stack().hurtAndBreak(1, serverLevel, serverPlayer, brokenItem -> {
                // A1 mirror of vanilla LivingEntity.onEquippedItemBroken:
                //   level.broadcastEntityEvent(this, entityEventForEquipmentBreak(inSlot));
                //   stopLocationBasedEffects(getItemBySlot(inSlot), inSlot, attributes);
                // Byte 50 == entityEventForEquipmentBreak(CHEST). Plays break VFX on
                // tracking clients at the chest area.
                player.level().broadcastEntityEvent(player, (byte) 50);

                // Inline stopLocationBasedEffects using the PRE-BREAK snapshot instead of
                // getItemBySlot(CHEST). Vanilla's version would strip modifiers from the
                // chestplate the player is wearing in the real CHEST slot; we only want to
                // remove modifiers contributed by the broken elytra itself.
                AttributeMap attributes = player.getAttributes();
                preBreakSnapshot.forEachModifier(EquipmentSlot.CHEST, (attribute, modifier) -> {
                    AttributeInstance instance = attributes.getInstance(attribute);
                    if (instance != null) {
                        instance.removeModifier(modifier);
                    }
                });
                // Vanilla's stopLocationBasedEffects also stops enchantment-based effects.
                EnchantmentHelper.stopLocationBasedEffects(preBreakSnapshot, player, EquipmentSlot.CHEST);
                ElytraSlotConstants.LOGGER.info("[elytraslot] custom-slot elytra broke item={}", brokenItem);
            });
        }
    }
}
