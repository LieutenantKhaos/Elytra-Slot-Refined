package com.warwa.elytraslot.host;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.Gliders;
import com.warwa.elytraslot.net.HostSyncPayload;
import com.warwa.elytraslot.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side join logic, called by each loader's join hook.
 *
 * <p>The host sync is sent immediately. The relocation runs one tick later
 * (loader glue defers it via server.execute) so inventory NBT and any accessory
 * mod's data are fully loaded before deciding.
 */
public final class JoinHandling {

    private JoinHandling() {
    }

    /**
     * Carries a not-yet-placeable legacy 2.x elytra across an entity clone
     * (respawn, End exit portal). Respawning builds a brand-new Player and
     * vanilla copies only its own fields, so without this the pending stack —
     * which lives only in memory until it can be placed — would be destroyed.
     */
    public static void copyPendingLegacyElytra(Player original, Player copy) {
        if (original instanceof LegacyElytraCarrier from
            && copy instanceof LegacyElytraCarrier to) {
            to.elytraslot$setPendingLegacyElytra(from.elytraslot$getPendingLegacyElytra());
        }
    }

    /** Announce the active host and panel visibility to the joining client. */
    public static void sendHostSync(ServerPlayer player) {
        Platform.get().sendToPlayer(player,
            new HostSyncPayload(ElytraHosts.server().id(), ElytraHosts.panelVisible(false)));
    }

    /**
     * A BODY-slot elytra is "stranded" when an external host is active and the
     * panel slot is hidden — the player would have no way to see or remove it
     * (e.g. the server switched from builtin to Trinkets/Curios, or the panel
     * config was turned off while it held an elytra). Relocates it down a
     * cascade that never creates a double equip: external slot, then empty
     * chest, then inventory, then drop.
     */
    public static void relocateStrandedElytra(ServerPlayer player) {
        ElytraHost host = ElytraHosts.server();
        if (host.isBuiltin() || ElytraHosts.panelVisible(false)) {
            return;
        }
        ItemStack stranded = player.getItemBySlot(EquipmentSlot.BODY);
        if (!Gliders.isGlider(stranded)) {
            return;
        }
        ItemStack moved = stranded.copy();
        player.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);

        // Must be empty, not merely glider-free: writing over any occupant would
        // destroy it.
        if (host.get(player).isEmpty() && host.set(player, moved, true)
            && ItemStack.isSameItemSameComponents(host.get(player), moved)) {
            ElytraSlot.LOGGER.info("Relocated stranded elytra of {} into the {} slot",
                player.getName().getString(), host.id());
            return;
        }
        // Only fall back to the chest when nothing else wears a glider, otherwise
        // this cascade would itself create the double equip it exists to avoid.
        if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            && !Gliders.isGlider(host.get(player))) {
            player.setItemSlot(EquipmentSlot.CHEST, moved);
            return;
        }
        if (player.getInventory().add(moved)) {
            return;
        }
        player.drop(moved, false);
    }
}
