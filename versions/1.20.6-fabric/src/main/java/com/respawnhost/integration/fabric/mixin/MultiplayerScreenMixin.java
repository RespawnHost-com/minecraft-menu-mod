package com.respawnhost.integration.fabric.mixin;

import com.respawnhost.core.LangKeys;
import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.ui.OrderScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    private static final int RESPAWNHOST_BUTTON_WIDTH = 110;
    private static final int RESPAWNHOST_BUTTON_HEIGHT = 20;
    private static final int RESPAWNHOST_MARGIN = 6;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void respawnhost$addOrderButton(CallbackInfo ci) {
        if (!RespawnConfig.get().showOrderButton()) {
            return;
        }
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable(LangKeys.ORDER_BUTTON),
                        button -> MinecraftClient.getInstance().setScreen(new OrderScreen(this)))
                .dimensions(this.width - RESPAWNHOST_BUTTON_WIDTH - RESPAWNHOST_MARGIN, RESPAWNHOST_MARGIN,
                        RESPAWNHOST_BUTTON_WIDTH, RESPAWNHOST_BUTTON_HEIGHT)
                .build());
    }
}
