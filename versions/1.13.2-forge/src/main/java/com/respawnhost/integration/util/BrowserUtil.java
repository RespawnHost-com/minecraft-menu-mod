package com.respawnhost.integration.util;

import com.respawnhost.integration.RespawnHostIntegrationMod;
import net.minecraft.util.Util;

import java.awt.Desktop;
import java.net.URI;

public final class BrowserUtil {
    private BrowserUtil() {
    }

    public static void open(String url) {
        try {
            Util.getOSType().openURI(url);
            return;
        } catch (Throwable t) {
            RespawnHostIntegrationMod.LOGGER.warn("Failed to open URL via OS handler: {}", url, t);
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Throwable t) {
            RespawnHostIntegrationMod.LOGGER.warn("Failed to open URL via AWT Desktop: {}", url, t);
        }
    }
}
