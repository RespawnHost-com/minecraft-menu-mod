package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;

public final class MultiplayerButtonInjector {
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 6;

    private MultiplayerButtonInjector() {
    }

    public static void onInitScreen(GuiScreenEvent.InitGuiEvent.Post event) {
        Screen screen = event.getGui();
        if (!(screen instanceof MultiplayerScreen)) {
            return;
        }
        if (!RespawnConfig.get().showOrderButton()) {
            return;
        }
        event.addWidget(new Button(screen.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.get(LangKeys.ORDER_BUTTON),
                button -> Minecraft.getInstance().setScreen(new OrderScreen(screen))));
    }
}
