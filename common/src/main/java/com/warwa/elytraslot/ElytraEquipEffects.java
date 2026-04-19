package com.warwa.elytraslot;

import com.warwa.elytraslot.mixin.LivingEntityAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.advancements.CriteriaTriggers;

/**
 * Exact mirror of BOTH vanilla equip side-effect paths for writes that bypass
 * {@code Player.setItemSlot} (which would run {@code onEquipItem} and the tick-driven
 * {@code collectEquipmentChanges} pipeline for us). Because our custom slot is not in
 * {@link EquipmentSlot#VALUES}, vanilla's tick diff never sees it, so we must manually:
 *
 * <ol>
 *   <li>Play the equip sound and fire {@link GameEvent#EQUIP}/{@link GameEvent#UNEQUIP}
 *       (mirrors {@code LivingEntity.onEquipItem}).</li>
 *   <li>Remove modifiers + stop location-based enchantment effects from the OLD stack
 *       (mirrors {@code LivingEntity.stopLocationBasedEffects} — called in pass 1 of
 *       {@code collectEquipmentChanges}).</li>
 *   <li>Apply modifiers (remove-by-id + addTransientModifier) + run location-based
 *       enchantment effects for the NEW stack (mirrors pass 2 of
 *       {@code collectEquipmentChanges}).</li>
 *   <li>Fire {@code CriteriaTriggers.INVENTORY_CHANGED} so "wear an elytra" style
 *       advancements can progress. (C2 fix.)</li>
 * </ol>
 *
 * All of the above run server-side only.
 *
 * Every branch emits a {@code [elytraslot]} debug log so the live run can be verified.
 */
public final class ElytraEquipEffects {
    private ElytraEquipEffects() {}

    /**
     * Slot identifier we report to vanilla's attribute/enchantment pipeline. Modifiers on
     * the elytra-stack are keyed to {@link EquipmentSlot#CHEST} (Equippable.slot() is
     * CHEST for every vanilla glider) so we reuse CHEST here for parity.
     */
    private static final EquipmentSlot EFFECTIVE_SLOT = EquipmentSlot.CHEST;

    public static void onSlotChanged(Player player, ItemStack oldStack, ItemStack newStack) {
        if (player == null) return;
        if (player.level().isClientSide()) return;
        if (player.isSpectator()) return;
        if (ItemStack.isSameItemSameComponents(oldStack, newStack)) return;
        // Vanilla parity: skip if the entity is on its very first tick (freshly spawned
        // / loaded). Matches the !firstTick guard in LivingEntity.onEquipItem.
        if (((LivingEntityAccessor) player).elytraslot$isFirstTick()) return;

        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] onSlotChanged ENTRY old={} new={} player={} firstTick={} silent={} creative={}",
            oldStack, newStack, player.getName().getString(),
            ((LivingEntityAccessor) player).elytraslot$isFirstTick(),
            player.isSilent(), player.isCreative()
        );

        // ─── Step 1: sound + game event (mirror LivingEntity.onEquipItem) ──────────────
        Equippable newEquippable = newStack.get(DataComponents.EQUIPPABLE);
        if (!player.isSilent() && newEquippable != null && newEquippable.slot() == EFFECTIVE_SLOT) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged playing equip sound={}",
                newEquippable.equipSound()
            );
            player.level().playSeededSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                newEquippable.equipSound(),
                player.getSoundSource(),
                1.0F, 1.0F,
                player.getRandom().nextLong()
            );
        } else {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged equip-sound skipped silent={} equippable-null={} slot-mismatch={}",
                player.isSilent(),
                newEquippable == null,
                newEquippable != null && newEquippable.slot() != EFFECTIVE_SLOT
            );
        }
        // doesEmitEquipEvent(slot) returns true for CHEST in vanilla LivingEntity.
        // GameEvent.EQUIP / UNEQUIP are Holder<GameEvent> in 26.1; gameEvent() takes Holder.
        Holder<GameEvent> gameEvent = newEquippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP;
        ElytraSlotConstants.LOGGER.info("[elytraslot] onSlotChanged gameEvent={}", gameEvent);
        player.gameEvent(gameEvent);

        // ─── Step 2: stop OLD stack's attribute modifiers + location-based effects ─────
        // Mirror LivingEntity.stopLocationBasedEffects(oldStack, slot, attrs).
        AttributeMap attrs = player.getAttributes();
        if (!oldStack.isEmpty()) {
            int[] removedCount = {0};
            oldStack.forEachModifier(EFFECTIVE_SLOT, (attrHolder, modifier) -> {
                AttributeInstance inst = attrs.getInstance(attrHolder);
                if (inst != null) {
                    // removeModifier(AttributeModifier) is void in 26.1 — no boolean return.
                    inst.removeModifier(modifier);
                    removedCount[0]++;
                }
            });
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged stopped OLD modifiers count={} stack={}",
                removedCount[0], oldStack
            );
            if (player.level() instanceof ServerLevel sl) {
                ElytraSlotConstants.LOGGER.info(
                    "[elytraslot] onSlotChanged stopLocationBasedEffects OLD stack={}", oldStack
                );
                EnchantmentHelper.stopLocationBasedEffects(oldStack, player, EFFECTIVE_SLOT);
            }
        } else {
            ElytraSlotConstants.LOGGER.info("[elytraslot] onSlotChanged OLD stack empty — skipped stop phase");
        }

        // ─── Step 3: apply NEW stack's attribute modifiers + run location-based effects ─
        // Mirror pass 2 of LivingEntity.collectEquipmentChanges.
        if (!newStack.isEmpty() && !newStack.isBroken()) {
            int[] appliedCount = {0};
            newStack.forEachModifier(EFFECTIVE_SLOT, (attrHolder, modifier) -> {
                AttributeInstance inst = attrs.getInstance(attrHolder);
                if (inst != null) {
                    inst.removeModifier(modifier.id()); // by id — vanilla safety: dedupe
                    inst.addTransientModifier(modifier);
                    appliedCount[0]++;
                }
            });
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged applied NEW modifiers count={} stack={}",
                appliedCount[0], newStack
            );
            if (player.level() instanceof ServerLevel sl) {
                ElytraSlotConstants.LOGGER.info(
                    "[elytraslot] onSlotChanged runLocationChangedEffects NEW stack={}", newStack
                );
                EnchantmentHelper.runLocationChangedEffects(sl, newStack, player, EFFECTIVE_SLOT);
            }
        } else {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged NEW stack empty/broken — skipped apply phase empty={} broken={}",
                newStack.isEmpty(), newStack.isBroken()
            );
        }

        // ─── Step 4: advancement trigger (C2 fix) ──────────────────────────────────────
        if (player instanceof ServerPlayer sp) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged firing CriteriaTriggers.INVENTORY_CHANGED for {}",
                sp.getName().getString()
            );
            CriteriaTriggers.INVENTORY_CHANGED.trigger(sp, sp.getInventory(), newStack);
        }

        // ─── Step 5: broadcast to tracking clients (F fix) ─────────────────────────────
        // The loader-specific sync dispatcher (Fabric: ElytraSlotNetworkFabric; NeoForge:
        // ElytraSlotNetworkNeoForge) installs itself into this hook at mod init. If no
        // dispatcher is registered (e.g. common-only test), the broadcast is a no-op and
        // a WARN is logged so we notice.
        ElytraSyncDispatcher dispatcher = ElytraSyncDispatcher.get();
        if (dispatcher != null) {
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] onSlotChanged broadcasting sync payload to trackers of {}",
                player.getName().getString()
            );
            dispatcher.broadcastSlotChange(player, newStack);
        } else {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] onSlotChanged NO sync dispatcher registered — remote clients will NOT see this change"
            );
        }

        ElytraSlotConstants.LOGGER.info("[elytraslot] onSlotChanged EXIT");
    }
}
