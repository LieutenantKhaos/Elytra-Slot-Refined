package com.warwa.elytraslot.client;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotSyncPayload;
import com.warwa.elytraslot.IElytraSlotPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Loader-neutral client-side handler for {@link ElytraSlotSyncPayload}. Both the
 * Fabric and NeoForge client entry points call this with the payload on the main
 * client thread.
 *
 * <p>The handler looks up the target {@link Player} in the client level (by UUID),
 * then writes the stack into the player's {@link com.warwa.elytraslot.ElytraSlotContainer}
 * via {@code setItemSilent(0, stack)} — silent because the server already broadcast the
 * equip sound to nearby clients; we do NOT want a double sound on the receiving client.
 *
 * <p>Every code path emits a {@code [elytraslot]} debug log for runtime verification.
 */
public final class ElytraSlotSyncClient {
    private ElytraSlotSyncClient() {}

    public static void handle(ElytraSlotSyncPayload payload) {
        UUID id = payload.playerId();
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] SyncClient.handle received payload for {} but no client level available",
                id
            );
            return;
        }
        Entity entity = mc.level.getPlayerByUUID(id);
        if (!(entity instanceof Player target)) {
            ElytraSlotConstants.LOGGER.warn(
                "[elytraslot] SyncClient.handle could not locate player {} in client level (entity={})",
                id, entity
            );
            return;
        }
        var container = ((IElytraSlotPlayer) target).elytraslot_getElytraContainer();
        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] SyncClient.handle writing stack={} into player={} containerId={}",
            payload.stack(), target.getName().getString(),
            Integer.toHexString(System.identityHashCode(container))
        );
        container.setItemSilent(0, payload.stack());
    }
}
