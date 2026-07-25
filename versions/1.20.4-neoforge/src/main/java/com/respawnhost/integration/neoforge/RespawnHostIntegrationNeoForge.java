package com.respawnhost.integration.neoforge;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.ui.OrderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RespawnHostIntegrationNeoForge.MOD_ID)
public final class RespawnHostIntegrationNeoForge {
    public static final String MOD_ID = "respawnhost_integration";
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 6;

    public RespawnHostIntegrationNeoForge() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            Screen screen = event.getScreen();
            if (!(screen instanceof JoinMultiplayerScreen)) {
                return;
            }
            if (!RespawnConfig.get().showOrderButton()) {
                return;
            }
            event.addListener(Button.builder(
                            Component.translatable(LangKeys.ORDER_BUTTON),
                            button -> Minecraft.getInstance().setScreen(new OrderScreen(screen)))
                    .bounds(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        });
    }
}
