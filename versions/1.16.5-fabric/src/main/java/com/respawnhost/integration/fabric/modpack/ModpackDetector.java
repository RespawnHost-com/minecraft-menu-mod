package com.respawnhost.integration.fabric.modpack;

import com.respawnhost.integration.fabric.config.RespawnConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ModpackDetector {
    private static final Set<String> KNOWN_MARKER_IDS = new HashSet<>(
            Arrays.asList("modpack", "packname", "modpackname", "modpackinfo", "packinfo"));

    private ModpackDetector() {
    }

    public static String detectModpackName() {
        try {
            String packId = RespawnConfig.get().getPackId();
            if (packId != null && !packId.trim().isEmpty()) {
                return packId;
            }
        } catch (RuntimeException ignored) {
        }

        String fromProperty = System.getProperty("modpack.name");
        if (fromProperty != null && !fromProperty.trim().isEmpty()) {
            return fromProperty;
        }

        try {
            String fromEnv = System.getenv("MODPACK_NAME");
            if (fromEnv != null && !fromEnv.trim().isEmpty()) {
                return fromEnv;
            }
        } catch (SecurityException ignored) {
        }

        try {
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                String id = mod.getMetadata().getId();
                if (id != null && KNOWN_MARKER_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    String name = mod.getMetadata().getName();
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                String name = mod.getMetadata().getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains("modpack") && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }
}
