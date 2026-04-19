package com.warwa.elytraslot;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * S2C payload carrying an elytra-slot change for a specific player. Needed because
 * vanilla {@code ClientboundSetEquipmentPacket} does not include our custom slot
 * (it iterates {@link net.minecraft.world.entity.EquipmentSlot#VALUES} only).
 *
 * <p>Broadcast to every client tracking the affected player (including the player
 * themselves) whenever {@link ElytraEquipEffects#onSlotChanged} fires on the server.
 *
 * <p>Client handler writes the stack into the target player's {@code ElytraSlotContainer}
 * via {@code setItemSilent(0, stack)}, so the existing render-state extract picks it up
 * without any changes.
 */
public record ElytraSlotSyncPayload(UUID playerId, ItemStack stack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ElytraSlotSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ElytraSlotConstants.MOD_ID, "slot_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElytraSlotSyncPayload> STREAM_CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ElytraSlotSyncPayload::playerId,
            ItemStack.OPTIONAL_STREAM_CODEC, ElytraSlotSyncPayload::stack,
            ElytraSlotSyncPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
