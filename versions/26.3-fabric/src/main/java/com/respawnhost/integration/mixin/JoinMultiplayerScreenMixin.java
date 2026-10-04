package com.respawnhost.integration.mixin;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void respawnhost$addOrderButton(CallbackInfo ci) {
        MultiplayerButtonInjector.addToScreen((JoinMultiplayerScreen) (Object) this, this::addRenderableWidget);
    }
}
