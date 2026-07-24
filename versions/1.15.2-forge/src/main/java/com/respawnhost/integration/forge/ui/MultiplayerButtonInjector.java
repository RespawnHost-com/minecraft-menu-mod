package com.respawnhost.integration.forge.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.forge.config.RespawnConfig;
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

    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        Screen gui = event.getGui();
        if (!(gui instanceof MultiplayerScreen)) {
            return;
        }
        if (!RespawnConfig.get().isShowOrderButton()) {
            return;
        }
        event.addWidget(new Button(gui.width - BUTTON_WIDTH - MARGIN, MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.get(LangKeys.ORDER_BUTTON),
                button -> Minecraft.getInstance().setScreen(new OrderScreen(gui))));
    }
}
