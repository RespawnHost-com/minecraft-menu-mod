package com.respawnhost.integration.ui;

import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;

public final class MultiplayerButtonInjector {
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 6;

    private MultiplayerButtonInjector() {
    }

    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof JoinMultiplayerScreen)) {
            return;
        }
        if (!RespawnConfig.get().showOrderButton()) {
            return;
        }
        event.addListener(Button.builder(
                        Component.translatable("respawnhost_integration.order.button"),
                        button -> Minecraft.getInstance().setScreen(new OrderScreen(screen)))
                .bounds(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
}
