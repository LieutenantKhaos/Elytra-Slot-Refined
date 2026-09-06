package com.warwa.elytraslot;

import com.warwa.elytraslot.config.ElytraSlotConfig;
import com.warwa.elytraslot.host.ElytraHosts;
import com.warwa.elytraslot.platform.Platform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point. Each loader calls {@link #init} exactly once from its own
 * entry point, before registering networking and event hooks.
 */
public final class ElytraSlot {

    public static final String MOD_ID = "elytra_slot_refined";
    public static final Logger LOGGER = LoggerFactory.getLogger("Elytra Slot Refined");

    private ElytraSlot() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Loads the config and resolves which elytra host (builtin / trinkets / curios)
     * this game instance uses. Loader-specific hosts (Curios) must be registered by
     * the calling loader entry point BEFORE this method runs.
     */
    public static void init(Platform platform) {
        Platform.set(platform);
        ElytraSlotConfig.load(platform.configDir().resolve(MOD_ID + ".json"));
        ElytraHosts.resolve();
        LOGGER.info("Elytra Slot Refined initialized; active elytra host: {}",
            ElytraHosts.server().id());
    }
}
