package com.warwa.elytraslot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.warwa.elytraslot.ElytraSlot;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain-GSON config at {@code <configDir>/elytraslot.json}. Tolerant load: missing
 * file writes defaults, a broken file logs and keeps defaults. All fields are
 * primitives with defaults so partial files deserialize safely.
 */
public final class ElytraSlotConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile ElytraSlotConfig instance = new ElytraSlotConfig();
    private static Path path;

    /**
     * When an accessory mod (Trinkets/Curios) provides the elytra slot, also show the
     * mod's own inventory panel slot alongside it. Server-authoritative: the value is
     * synced to clients on join.
     */
    public boolean showInventoryPanel = false;

    /**
     * Which elytra host to prefer: "auto" (trinkets, then curios, then builtin),
     * or force one of "builtin", "trinkets", "curios". Falls back to auto order
     * when the forced host is not installed.
     */
    public String slotProvider = "auto";

    public static ElytraSlotConfig get() {
        return instance;
    }

    public static void load(Path file) {
        path = file;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                ElytraSlotConfig loaded = GSON.fromJson(reader, ElytraSlotConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                ElytraSlot.LOGGER.warn("Failed to read {}; keeping defaults", file, e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            ElytraSlot.LOGGER.warn("Failed to write {}", path, e);
        }
    }
}
