package com.respawnhost.integration.forge;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(RespawnHostIntegrationForge.MOD_ID)
public final class RespawnHostIntegrationForge {
    public static final String MOD_ID = "respawnhost_integration";

    public RespawnHostIntegrationForge() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MultiplayerButtonInjector::register);
    }
}
