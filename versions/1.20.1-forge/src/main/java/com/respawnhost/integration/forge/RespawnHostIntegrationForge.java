package com.respawnhost.integration.forge;

import com.respawnhost.integration.config.RespawnConfig;
import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(RespawnHostIntegrationForge.MOD_ID)
public final class RespawnHostIntegrationForge {
    public static final String MOD_ID = "respawnhost_integration";

    public RespawnHostIntegrationForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RespawnConfig.init(FMLPaths.CONFIGDIR.get());
            MinecraftForge.EVENT_BUS.addListener(MultiplayerButtonInjector::onScreenInitPost);
        }
    }
}
