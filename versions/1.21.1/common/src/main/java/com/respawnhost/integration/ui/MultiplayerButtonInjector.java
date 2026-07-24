package com.respawnhost.integration.ui;

import com.respawnhost.integration.config.RespawnConfig;
import dev.architectury.event.events.client.ClientGuiEvent;
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

    public static void register() {
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (!(screen instanceof JoinMultiplayerScreen)) {
                return;
            }
            if (!RespawnConfig.get().showOrderButton()) {
                return;
            }
            access.addRenderableWidget(Button.builder(
                            Component.translatable("respawnhost_integration.order.button"),
                            button -> Minecraft.getInstance().setScreen(new OrderScreen(screen)))
                    .bounds(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        });
    }
}
