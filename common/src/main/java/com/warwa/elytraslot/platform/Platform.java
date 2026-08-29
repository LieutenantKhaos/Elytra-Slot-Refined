package com.warwa.elytraslot.platform;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The small loader abstraction. Implemented once per loader and installed at init;
 * common code never touches loader APIs directly.
 */
public interface Platform {

    boolean isModLoaded(String modId);

    Path configDir();

    /** Sends an S2C payload to one player. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    static Platform get() {
        Platform platform = Holder.instance;
        if (platform == null) {
            throw new IllegalStateException("Platform not initialized");
        }
        return platform;
    }

    static void set(Platform platform) {
        Holder.instance = platform;
    }

    final class Holder {
        private static volatile Platform instance;

        private Holder() {
        }
    }
}
