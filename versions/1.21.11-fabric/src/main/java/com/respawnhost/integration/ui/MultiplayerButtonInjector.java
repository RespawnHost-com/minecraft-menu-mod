package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

public final class MultiplayerButtonInjector {
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 6;

    private MultiplayerButtonInjector() {
    }

    public static void addToScreen(JoinMultiplayerScreen screen, ButtonAdder adder) {
        if (!RespawnConfig.get().showOrderButton()) {
            return;
        }
        adder.add(Button.builder(
                        Component.translatable(LangKeys.ORDER_BUTTON),
                        button -> Minecraft.getInstance().setScreen(new OrderScreen(screen)))
                .bounds(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @FunctionalInterface
    public interface ButtonAdder {
        void add(Button button);
    }
}
