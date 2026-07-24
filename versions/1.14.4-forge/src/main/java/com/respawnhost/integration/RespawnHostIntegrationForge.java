package com.respawnhost.integration;

import com.respawnhost.integration.ui.MultiplayerButtonInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("respawnhost_integration")
public class RespawnHostIntegrationForge {
    public RespawnHostIntegrationForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(MultiplayerButtonInjector::onInitScreen);
        }
    }

    public static int loadedModCount() {
        try {
            return ModList.get().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
