package com.respawnhost.integration.neoforge;

import com.respawnhost.integration.RespawnHostIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = RespawnHostIntegration.MOD_ID, dist = Dist.CLIENT)
public final class RespawnHostIntegrationNeoForge {
    public RespawnHostIntegrationNeoForge() {
        RespawnHostIntegration.init();
    }
}
