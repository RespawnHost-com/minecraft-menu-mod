package com.respawnhost.integration.modpack;

import com.respawnhost.integration.config.RespawnConfig;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
            List<? extends IModInfo> mods = ModList.get().getMods();
            for (IModInfo mod : mods) {
                String id = mod.getModId();
                if (id != null && KNOWN_MARKER_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    String name = mod.getDisplayName();
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
            for (IModInfo mod : mods) {
                String name = mod.getDisplayName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains("modpack") && !name.isBlank()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }
}
