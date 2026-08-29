package com.warwa.elytraslot.net;

import com.warwa.elytraslot.ElytraSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C, sent once on join: which elytra host the server runs (builtin / trinkets /
 * curios) and whether the inventory panel slot is shown alongside an external host.
 * Server-authoritative — the client adopts both values for the session and resets
 * them on disconnect.
 */
public record HostSyncPayload(String hostId, boolean showPanel) implements CustomPacketPayload {

    public static final Type<HostSyncPayload> TYPE = new Type<>(ElytraSlot.id("host_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HostSyncPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), HostSyncPayload::hostId,
            ByteBufCodecs.BOOL, HostSyncPayload::showPanel,
            HostSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
