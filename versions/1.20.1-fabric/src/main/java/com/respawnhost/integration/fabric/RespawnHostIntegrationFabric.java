package com.respawnhost.integration.fabric;

import com.respawnhost.integration.config.RespawnConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class RespawnHostIntegrationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RespawnConfig.init(FabricLoader.getInstance().getConfigDir());
    }
}
