package com.respawnhost.integration.forge.ui;

import java.awt.Desktop;
import java.net.URI;

public final class BrowserUtil {
    private BrowserUtil() {
    }

    public static void open(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Throwable ignored) {
        }
    }
}
