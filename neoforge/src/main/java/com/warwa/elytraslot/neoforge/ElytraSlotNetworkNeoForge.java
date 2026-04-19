package com.warwa.elytraslot.neoforge;

import com.warwa.elytraslot.ElytraSlotConstants;
import com.warwa.elytraslot.ElytraSlotSyncPayload;
import com.warwa.elytraslot.ElytraSyncDispatcher;
import com.warwa.elytraslot.client.ElytraSlotSyncClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge-side wiring for the elytra-slot sync payload (F fix). Registered by the
 * mod entry point; listens for the NeoForge {@link RegisterPayloadHandlersEvent} on
 * the mod bus.
 *
 * <p>Registers the S2C payload and its client-side handler, then installs an
 * {@link ElytraSyncDispatcher} that delegates broadcast to
 * {@link PacketDistributor#sendToPlayersTrackingEntityAndSelf(net.minecraft.world.entity.Entity,
 * net.minecraft.network.protocol.common.custom.CustomPacketPayload,
 * net.minecraft.network.protocol.common.custom.CustomPacketPayload...)}.
 */
public final class ElytraSlotNetworkNeoForge {
    private ElytraSlotNetworkNeoForge() {}

    public static void register(IEventBus modBus) {
        ElytraSlotConstants.LOGGER.info("[elytraslot] NeoForge network init BEGIN");
        modBus.addListener(ElytraSlotNetworkNeoForge::onRegisterPayloads);

        ElytraSyncDispatcher.register((Player player, ItemStack newStack) -> {
            if (player.level().isClientSide()) {
                ElytraSlotConstants.LOGGER.warn(
                    "[elytraslot] NeoForgeDispatcher.broadcastSlotChange called client-side — skipping");
                return;
            }
            ElytraSlotSyncPayload payload = new ElytraSlotSyncPayload(player.getUUID(), newStack);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
            ElytraSlotConstants.LOGGER.info(
                "[elytraslot] NeoForgeDispatcher.broadcastSlotChange player={} stack={}",
                player.getName().getString(), newStack
            );
        });

        ElytraSlotConstants.LOGGER.info("[elytraslot] NeoForge network init END (dispatcher registered — handler registration pending payload event)");
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        ElytraSlotConstants.LOGGER.info("[elytraslot] NeoForge onRegisterPayloads BEGIN");
        PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToClient(
            ElytraSlotSyncPayload.TYPE,
            ElytraSlotSyncPayload.STREAM_CODEC,
            (payload, ctx) -> {
                // NeoForge default thread is the main game thread, but ctx.enqueueWork
                // is idiomatic in case future versions change the default.
                ctx.enqueueWork(() -> {
                    ElytraSlotConstants.LOGGER.info(
                        "[elytraslot] NeoForge S2C receive playerId={} stack={}",
                        payload.playerId(), payload.stack()
                    );
                    ElytraSlotSyncClient.handle(payload);
                });
            }
        );
        ElytraSlotConstants.LOGGER.info(
            "[elytraslot] NeoForge playToClient payload type registered: {}",
            ElytraSlotSyncPayload.TYPE.id()
        );
        ElytraSlotConstants.LOGGER.info("[elytraslot] NeoForge onRegisterPayloads END");
    }
}
