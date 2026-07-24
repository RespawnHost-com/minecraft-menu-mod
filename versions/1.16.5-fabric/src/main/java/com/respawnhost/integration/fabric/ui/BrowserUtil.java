package com.respawnhost.integration.fabric.ui;

import java.awt.Desktop;
import java.net.URI;

import net.minecraft.util.Util;

public final class BrowserUtil {
    private BrowserUtil() {
    }

    public static void open(String url) {
        try {
            Util.getOperatingSystem().open(url);
            return;
        } catch (Throwable ignored) {
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Throwable ignored) {
        }
    }
}
