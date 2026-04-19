package com.warwa.elytraslot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-agnostic contract for broadcasting an elytra-slot change from the server
 * to all clients that track the affected player. Each loader (Fabric, NeoForge)
 * registers a concrete implementation at mod init via {@link #register(ElytraSyncDispatcher)}.
 * {@link ElytraEquipEffects#onSlotChanged} then invokes {@link #get()} to dispatch.
 *
 * <p>This exists because vanilla {@code ClientboundSetEquipmentPacket} iterates
 * {@link net.minecraft.world.entity.EquipmentSlot#VALUES} only — our custom slot is
 * excluded. Without a custom packet, other clients would never render this player's
 * custom-slot elytra (deviation F in the audit).
 */
public interface ElytraSyncDispatcher {

    void broadcastSlotChange(Player player, ItemStack newStack);

    /** Holder is in a nested class so that the interface itself stays pure contract. */
    final class Holder {
        private Holder() {}
        static volatile ElytraSyncDispatcher instance = null;
    }

    /** Called once per loader at mod init. */
    static void register(ElytraSyncDispatcher dispatcher) {
        Holder.instance = dispatcher;
        ElytraSlotConstants.LOGGER.info("[elytraslot] ElytraSyncDispatcher registered: {}", dispatcher.getClass().getName());
    }

    /** Returns the registered dispatcher or null if none registered yet. */
    static ElytraSyncDispatcher get() {
        return Holder.instance;
    }
}
