package com.respawnhost.integration.forge;

import com.respawnhost.integration.forge.ui.MultiplayerButtonInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("respawnhost_integration")
public final class RespawnHostIntegrationForge {
    public RespawnHostIntegrationForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.addListener(MultiplayerButtonInjector::onInitGui));
    }
}
