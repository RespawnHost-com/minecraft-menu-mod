package com.respawnhost.integration.modpack;

import com.respawnhost.integration.config.RespawnConfig;
import dev.architectury.platform.Platform;
import dev.architectury.platform.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public final class ModpackDetector {
    private static final Set<String> KNOWN_MARKER_IDS = Set.of("modpack", "packname", "modpackname", "modpackinfo", "packinfo");

    private ModpackDetector() {
    }

    public static @Nullable String detectModpackName() {
        try {
            String packId = RespawnConfig.get().packId();
            if (packId != null && !packId.isBlank()) {
                return packId;
            }
        } catch (RuntimeException ignored) {
        }

        String fromProperty = System.getProperty("modpack.name");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }

        try {
            String fromEnv = System.getenv("MODPACK_NAME");
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv;
            }
        } catch (SecurityException ignored) {
        }

        try {
            Collection<Mod> mods = Platform.getMods();
            for (Mod mod : mods) {
                String id = mod.getModId();
                if (id != null && KNOWN_MARKER_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    String name = mod.getName();
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
            for (Mod mod : mods) {
                String name = mod.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains("modpack") && !name.isBlank()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }
}
