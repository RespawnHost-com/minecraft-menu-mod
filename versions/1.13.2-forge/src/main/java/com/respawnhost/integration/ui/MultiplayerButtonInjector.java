package com.respawnhost.integration.ui;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MultiplayerButtonInjector {
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 6;
    private static final int BUTTON_ID = 73571;

    private MultiplayerButtonInjector() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MultiplayerButtonInjector());
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiMultiplayer)) {
            return;
        }
        if (!RespawnConfig.get().showOrderButton()) {
            return;
        }
        event.addButton(new RunnableButton(BUTTON_ID, event.getGui().width - BUTTON_WIDTH - MARGIN, MARGIN,
                BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format(LangKeys.ORDER_BUTTON),
                () -> Minecraft.getInstance().displayGuiScreen(new OrderScreen(event.getGui()))));
    }
}
