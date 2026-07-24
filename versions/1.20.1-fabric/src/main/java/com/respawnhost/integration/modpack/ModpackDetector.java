package com.respawnhost.integration.modpack;

import com.respawnhost.integration.config.RespawnConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
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
            Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
            for (ModContainer mod : mods) {
                ModMetadata metadata = mod.getMetadata();
                String id = metadata.getId();
                if (id != null && KNOWN_MARKER_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    String name = metadata.getName();
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
            for (ModContainer mod : mods) {
                String name = mod.getMetadata().getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains("modpack") && !name.isBlank()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }

    public static int loadedModCount() {
        try {
            return FabricLoader.getInstance().getAllMods().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
