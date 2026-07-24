package com.respawnhost.integration.modpack;

import com.respawnhost.integration.config.RespawnConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ModpackDetector {
    private static final Set<String> KNOWN_MARKER_IDS = new HashSet<>(Arrays.asList(
            "modpack", "packname", "modpackname", "modpackinfo", "packinfo"));

    private ModpackDetector() {
    }

    public static String detectModpackName() {
        try {
            String packId = RespawnConfig.get().packId();
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
            List<? extends IModInfo> mods = ModList.get().getMods();
            for (IModInfo mod : mods) {
                String id = mod.getModId();
                if (id != null && KNOWN_MARKER_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                    String name = mod.getDisplayName();
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
            for (IModInfo mod : mods) {
                String name = mod.getDisplayName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains("modpack") && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }
}
