package com.respawnhost.integration.fabric;

import com.respawnhost.integration.config.RespawnConfig;
import net.fabricmc.api.ClientModInitializer;

public final class RespawnHostIntegrationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RespawnConfig.get();
    }
}
