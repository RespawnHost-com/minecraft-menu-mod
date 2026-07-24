package com.respawnhost.integration.forge;

import com.respawnhost.integration.config.RespawnConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(RespawnHostIntegrationForge.MOD_ID)
public final class RespawnHostIntegrationForge {
    public static final String MOD_ID = "respawnhost_integration";

    public RespawnHostIntegrationForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RespawnConfig::get);
    }
}
