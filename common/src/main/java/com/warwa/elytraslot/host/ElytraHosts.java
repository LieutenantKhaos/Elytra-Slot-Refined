package com.warwa.elytraslot.host;

import com.warwa.elytraslot.ElytraSlot;
import com.warwa.elytraslot.config.ElytraSlotConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves and holds the active elytra host.
 *
 * <p>Two views exist deliberately (this fixes a bug in the old mod where joining a
 * host-less server permanently corrupted a shared static for the rest of the game
 * session):
 * <ul>
 *   <li>{@link #server()} — what THIS game instance resolved at init. Immutable
 *       after init. All server-side logic uses it.</li>
 *   <li>{@link #client()} — what the connected server told us on join. Defaults to
 *       the local resolution and is reset on disconnect. All client-side logic
 *       (rendering, menu slot activity, client-side slot checks) uses it.</li>
 * </ul>
 */
public final class ElytraHosts {

    public static final BuiltinHost BUILTIN = new BuiltinHost();

    /** Registration order defines the auto-priority: trinkets, then curios. */
    private static final Map<String, ElytraHost> EXTERNAL = new LinkedHashMap<>();

    private static ElytraHost resolved = BUILTIN;
    private static volatile ElytraHost clientOverride;
    private static volatile Boolean clientShowPanel;

    private ElytraHosts() {
    }

    /** Called by compat initializers before {@link ElytraSlot#init} resolves. */
    public static void registerExternal(ElytraHost host) {
        EXTERNAL.put(host.id(), host);
    }

    public static void resolve() {
        String preferred = ElytraSlotConfig.get().slotProvider;
        if ("builtin".equals(preferred)) {
            resolved = BUILTIN;
            return;
        }
        ElytraHost forced = EXTERNAL.get(preferred);
        if (forced != null) {
            resolved = forced;
            return;
        }
        if (!"auto".equals(preferred)) {
            ElytraSlot.LOGGER.warn("Config slotProvider '{}' is not available; using auto order", preferred);
        }
        resolved = EXTERNAL.values().stream().findFirst().orElse(BUILTIN);
    }

    /** The host this game instance resolved at init; server-side ground truth. */
    public static ElytraHost server() {
        return resolved;
    }

    /** Whether any external host (Trinkets/Curios) is installed in this instance. */
    public static boolean hasExternalInstalled() {
        return !EXTERNAL.isEmpty();
    }

    /** The host governing the world the client currently sees. */
    public static ElytraHost client() {
        ElytraHost override = clientOverride;
        return override != null ? override : resolved;
    }

    /**
     * Host for logic running on the given entity's side. Accessory mods query slot
     * eligibility with no wearer (a null entity); those queries answer from the
     * locally resolved host.
     */
    public static ElytraHost forSide(@Nullable Entity entity) {
        if (entity == null) {
            return resolved;
        }
        return entity.level().isClientSide() ? client() : resolved;
    }

    /**
     * Whether the mod's own inventory panel slot is visible. Always true with the
     * builtin host (it is then the only slot); with an external host it follows the
     * server's showInventoryPanel config.
     */
    public static boolean panelVisible(boolean clientSide) {
        ElytraHost host = clientSide ? client() : server();
        if (host.isBuiltin()) {
            return true;
        }
        Boolean synced = clientSide ? clientShowPanel : null;
        return synced != null ? synced : ElytraSlotConfig.get().showInventoryPanel;
    }

    /** Applies the panel visibility announced by the server on join. */
    public static void applyClientPanel(boolean showPanel) {
        clientShowPanel = showPanel;
    }

    /** Applies the host id announced by the server on join. */
    public static void applyClientHost(String id) {
        if (resolved.id().equals(id)) {
            clientOverride = null;
        } else if (BuiltinHost.ID.equals(id)) {
            clientOverride = BUILTIN;
        } else if (EXTERNAL.containsKey(id)) {
            clientOverride = EXTERNAL.get(id);
        } else {
            // Server uses a host this client doesn't have installed. Since Trinkets and
            // Curios are required on the client when the server uses them, this only
            // happens with mismatched installs; fall back to builtin and say so.
            ElytraSlot.LOGGER.warn("Server announced unknown elytra host '{}'; using builtin", id);
            clientOverride = BUILTIN;
        }
    }

    /** Called on client disconnect so a later singleplayer world is unaffected. */
    public static void resetClient() {
        clientOverride = null;
        clientShowPanel = null;
    }
}
